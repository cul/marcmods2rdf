package edu.mit.simile.rdfizer.marcmods.impl;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.mit.simile.rdfizer.marcmods.Main;


public class MODS2RDF extends AbstractTransform {

    private static final int DEFAULT_SLOT_SIZE = 1;

    static final String MODSXML_2_MODSRDF = "mods2rdf.xslt";
    static final String WEB_MODSXML_2_MODSRDF = "http://simile.mit.edu/repository/RDFizers/marcmods2rdf/stylesheets/mods2rdf.xslt";

    private final Templates m_templates;

    public MODS2RDF(File transformationDir) throws TransformerConfigurationException {
        this(null, transformationDir);
    }

    public MODS2RDF(AbstractTransform sourceTransform, File transformationDir) throws TransformerConfigurationException {
        super(sourceTransform);
        m_templates = getTemplates(transformationDir, MODSXML_2_MODSRDF, WEB_MODSXML_2_MODSRDF);
    }

    @Override
    protected void doTransform(Source input, Result output) throws TransformerException {
        Transformer t = m_templates.newTransformer();
        t.transform(input, output);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        CommandLineParser parser = new PosixParser();

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

        try {
            CommandLine line = parser.parse(options, args);
            String[] clean_args = line.getArgs();
            File transformations = null;
            if (line.hasOption("t")) {
                transformations = new File(line.getOptionValue("t"));
                if (!transformations.exists()) Main.fatal("Transformation file '" + transformations + "' does not exist.");
                if (!transformations.isDirectory())
                    Main.fatal("Transformation file '" + transformations + "' must be a folder.");
            }
            File input = new File(clean_args[0]);
            TransformerFactory factory = TransformerFactory.newInstance();
            if (!factory.getFeature(SAXTransformerFactory.FEATURE)) {
                throw new UnsupportedOperationException("SAXTransformerFactory is not supported");
            }

            SAXTransformerFactory saxFactory = (SAXTransformerFactory) factory;
            Transformer modsxml2modsrdf = saxFactory.newTemplates(
                    new StreamSource(new File(transformations, MODSXML_2_MODSRDF))).newTransformer();

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } finally {}

    }

}
