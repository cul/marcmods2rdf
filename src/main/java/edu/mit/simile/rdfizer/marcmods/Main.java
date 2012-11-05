package edu.mit.simile.rdfizer.marcmods;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;
import org.w3c.dom.Document;

import edu.mit.simile.rdfizer.marcmods.impl.AbstractTransform;
import edu.mit.simile.rdfizer.marcmods.impl.DOM2XML;
import edu.mit.simile.rdfizer.marcmods.impl.MARC2MODS;
import edu.mit.simile.rdfizer.marcmods.impl.MODS2RDF;

/*
 * @author Stefano Mazzocchi
 */

public class Main {

    static final String WEB_MARCXML_2_MODSXML = "http://www.loc.gov/standards/mods/v3/MARC21slim2MODS3.xsl";

    static final String WEB_MODSXML_2_MODSRDF = "http://simile.mit.edu/repository/RDFizers/marcmods2rdf/stylesheets/mods2rdf.xslt";

    static final String MARCXML_2_MODSXML = "MARC21slim2MODS3.xsl";

    static final String MODSXML_2_MODSRDF = "mods2rdf.xslt";

    static final int DEFAULT_SLOT_SIZE = 1000;

    public static void main(String args[]) throws Exception {
        new Main().process(args);
    }

    public static void fatal(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    AbstractTransform dom2xml;

    AbstractTransform marc2mods;

    AbstractTransform mods2rdf;

    int slot_size = DEFAULT_SLOT_SIZE;

    int predicted_slots = 1;

    int slot = 0;

    int counter = 0;

    int count = 1;

    public static Options getCommandLineOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "show this help screen");
        options.addOption("n", "normalize", false, "perform Unicode normalization");
        options.addOption("c", "save-marc", false, "serialize the parsed MARC records in Z39.2 format");
        options.addOption("m", "save-marcxml", false, "serialize the parsed MARC records in MARCXML");
        options.addOption("o", "save-mods", false, "serialize the parsed MARC records coverted in MODS/XML");
        options.addOption("r", "no-rdf", false, "do NOT perform the MODS/XML/RDF transformation");
        options.addOption("s", "slot-size <size>", true, "how many records are stored in a slot (default: "
                + DEFAULT_SLOT_SIZE + ")");
        options.addOption("t", "transformations <folder>", true,
                "use local transformations instead of fetching the latest ones from the web");
        return options;
    }

    public void process(String[] args) throws Exception {

        File input = null;
        File output = null;
        File transformations = null;

        boolean normalize = false;
        boolean do_marc = false;
        boolean do_marcxml = false;
        boolean do_modsxml = false;
        boolean do_modsrdfxml = true;

        CommandLineParser parser = new PosixParser();
        Options options = getCommandLineOptions();

        try {
            CommandLine line = parser.parse(options, args);
            String[] clean_args = line.getArgs();

            normalize = line.hasOption("n");
            do_marc = line.hasOption("c");
            do_marcxml = line.hasOption("m");
            do_modsxml = line.hasOption("o");
            do_modsrdfxml = !line.hasOption("r");

            if (line.hasOption("t")) {
                transformations = new File(line.getOptionValue("t"));
                if (!transformations.exists()) fatal("Transformation file '" + transformations + "' does not exist.");
                if (!transformations.isDirectory())
                    fatal("Transformation file '" + transformations + "' must be a folder.");
            }

            if (line.hasOption("s")) {
                try {
                    slot_size = Integer.parseInt(line.getOptionValue("s"));
                } catch (Exception e) {
                    fatal("Slot size must be a number.");
                }
            }

            if (line.hasOption("help") || clean_args.length < 3) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("marcmods2rdf [options] input.mrc count output_folder", options);
                System.exit(1);
            }

            input = new File(clean_args[0]);
            if (!input.exists()) fatal("Input file '" + input + "' does not exist.");
            if (!input.canRead()) fatal("You don't have permission to read the input file '" + input + "'.");
            if (!input.isFile())
                fatal("Input file '" + input + "' is a directory, I can only read single files as input.");

            try {
                count = Integer.parseInt(clean_args[1]);
            } catch (Exception e) {
                fatal("Record count must be a number.");
            }

            output = new File(clean_args[2]);
            if (!output.exists()) output.mkdirs();
            if (!output.isDirectory())
                fatal("Output file '" + output + "' is a file, I can only write output to a folder.");
            if (!output.canWrite()) fatal("You don't have permission to write to the output folder '" + output + "'.");
        } catch (ParseException exp) {
            System.err.println("Unexpected exception:" + exp.getMessage());
        }

        predicted_slots = count / slot_size;

        MarcXmlWriter xmlWriter = null;
        Record record = null;

        File marcxml = null;
        File modsxml = null;
        File modsrdfxml = null;

        Result result = null;
        Document dom;
        Source source;

        boolean open = false;
        boolean terminate = false;

        String basepath = output.getAbsolutePath();

        log(START);

        dom2xml = do_marcxml || do_modsxml ? new DOM2XML() : null;
        marc2mods = do_modsxml ? new MARC2MODS(transformations) : null;
        mods2rdf = do_modsrdfxml ? new MODS2RDF(transformations) : null;

        SourceIterator sourceIter =  new SourceIterator(
                                         new MarcStreamReader(new BufferedInputStream(new FileInputStream(input))),
                                         (do_marc)?basepath:null,
                                         this, count);
        sourceIter.setNormalize(normalize);
        while (sourceIter.hasNext()) {
           source = sourceIter.next();
           try {
               if (do_marcxml || do_modsxml || do_modsrdfxml) {
                   // serialize the DOM representation of a MARC record in MARCXML
                   if (do_marcxml) {
                       marcxml = getPath(basepath, "marc_xml", counter, count, "mrc.xml");
                       serializeTransform(dom2xml, source, marcxml, this, WRITE_MARCXML);
                   }

                   if (do_modsxml || do_modsrdfxml) {

                       // transform the MARC record into MODS/XML
                       log(TRANSFORM_MARCXML);
                       result = new DOMResult();
                       marc2mods.transform(source, result);
                       dom = (Document) ((DOMResult) result).getNode();
                       source = new DOMSource(dom);

                       // serialize the MODS/XML DOM
                       if (do_modsxml) {
                           modsxml = getPath(basepath, "mods_xml", counter, count, "mods.xml");
                           serializeTransform(dom2xml, source, modsxml, this, WRITE_MODSXML);
                       }

                       if (do_modsrdfxml) {
                           // transform the MODS/XML record into MODS/RDF and serialize
                           modsrdfxml = getPath(basepath, "mods_rdf_xml", counter, count, "mods.rdf.xml");
                           serializeTransform(mods2rdf, source, modsrdfxml, this, WRITE_MODSRDF);
                       }

                   }
               }
           } catch (Exception e) {
                   error("Failed reading record [" + counter + "]", e);
           }
        }
        log(END);
    }

    private static void serializeTransform(AbstractTransform transform, Source input, File output, Main main, int logOp) throws TransformerException, IOException {
        main.log(logOp);
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output));
        Result result = new StreamResult(outputStream);
        transform.transform(input, result);
        outputStream.close();
    }

    static File getPath(String base, String type, int counter, int count, String extension) {
        String counter_str = Integer.toString(counter);
        int counter_len = counter_str.length();
        int count_len = Integer.toString(count).length() - 1;
        StringBuffer buf = new StringBuffer();
        buf.append(base);
        buf.append(File.separatorChar);
        buf.append(type);
        buf.append(File.separatorChar);
        for (int i = 0; i < count_len; i++) {
            char c = (counter_len > i) ? counter_str.charAt(i) : '0';
            buf.append(c);
            buf.append(File.separatorChar);
        }
        File path = new File(buf.toString());
        if (!path.exists()) path.mkdirs();
        return new File(path, counter + "." + extension);
    }

    static final int START = 0;

    static final int START_SLOT = 1;

    static final int READ = 2;

    static final int WRITE = 3;

    static final int WRITE_MARC = 4;

    static final int WRITE_MARCXML = 5;

    static final int TRANSFORM_MARCXML = 6;

    static final int WRITE_MODSXML = 7;

    static final int TRANSFORM_MODSXML = 8;

    static final int WRITE_MODSRDF = 9;

    static final int FINISH_SLOT = 10;

    static final int END = 11;

    long start;

    long slot_start;

    long elapsed;

    long speed;

    void log(int action) {
        switch (action) {
            case START:
                start = System.currentTimeMillis();
                break;
            case START_SLOT:
                slot_start = System.currentTimeMillis();
                out("[" + slot + "]  ");
                break;
            case READ:
                switch (counter % 4) {
                    case 0:
                        out('|');
                        break;
                    case 1:
                        out('/');
                        break;
                    case 2:
                        out('-');
                        break;
                    case 3:
                        out('\\');
                        break;
                }
                break;
            case WRITE:
                break;
            case WRITE_MARC:
                out('w');
                break;
            case WRITE_MARCXML:
                out('w');
                break;
            case TRANSFORM_MARCXML:
                out('*');
                break;
            case WRITE_MODSXML:
                out('w');
                break;
            case TRANSFORM_MODSXML:
                out('*');
                break;
            case WRITE_MODSRDF:
                out('w');
                break;
            case FINISH_SLOT:
                elapsed = System.currentTimeMillis() - start;
                speed = counter * 1000 / elapsed;
                long estimated_time = ((count - counter) * 1000) / speed;
                int perc = counter * 100 / count;
                String perc_str = ((perc < 100) ? " " : "") + ((perc < 10) ? " " : "") + Integer.toString(perc)
                        + "% completed - ";
                out("\b" + perc_str + speed + " rec/sec - " + prettyTime(estimated_time) + " time to end\n");
                break;
            case END:
                elapsed = System.currentTimeMillis() - start;
                speed = counter * 1000 / elapsed;
                System.out.println("\nProcessed " + counter + " records in " + elapsed / 1000 + " seconds (" + speed
                        + " rec/sec)");
                break;
        }
    }

    void error(String msg, Exception e) {
        System.err.println("\n" + msg + ": " + e.getMessage());
        e.printStackTrace(System.err);
    }

    void out(char c) {
        System.out.print('\b');
        System.out.print(c);
    }

    void out(String msg) {
        System.out.print(msg);
    }

    String prettyTime(long millis) {
        StringBuffer buf = new StringBuffer();

        long days = millis / (1000 * 60 * 60 * 24);
        if (days < 10) buf.append('0');
        buf.append(days);
        buf.append('d');
        millis %= (1000 * 60 * 60 * 24);

        long hours = millis / (1000 * 60 * 60);
        if (hours < 10) buf.append('0');
        buf.append(hours);
        buf.append('h');
        millis %= (1000 * 60 * 60);

        long minutes = millis / (1000 * 60);
        if (minutes < 10) buf.append('0');
        buf.append(minutes);
        buf.append('m');
        millis %= (1000 * 60);

        long seconds = millis / 1000;
        if (seconds < 10) buf.append('0');
        buf.append(seconds);
        buf.append('s');

        return buf.toString();
    }
}
