package edu.mit.simile.rdfizer.marcmods;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;


public class SourceIterator implements Iterator<Source>{
    private final MarcStreamReader m_delegate;
    private final String m_basePath;
    private final Main m_logProxy;
    private boolean m_normalize;
    private final int m_limit;
    private int counter = 0;
    public SourceIterator(MarcStreamReader marcReader, String basePath, Main logProxy, int limit) {
        m_delegate = marcReader;
        m_basePath = basePath;
        m_logProxy = logProxy;
        m_limit = limit;
    }

    public void setNormalize(boolean normalize){
        m_normalize = normalize;
    }

    public boolean hasNext() {
        return (counter < m_limit) && m_delegate.hasNext();
    }

    public Source next() {
        if (counter + 1 < m_limit) return null;
        DOMResult result = new DOMResult();
        MarcXmlWriter xmlWriter = new MarcXmlWriter(result);
        xmlWriter.setUnicodeNormalization(m_normalize);
        Record record = m_delegate.next();
        m_logProxy.log(Main.READ);
        counter++;
        xmlWriter.write(record);
        m_logProxy.log(Main.WRITE);
        try {
            serializeRecord(record);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DOMSource(result.getNode());
    }

    public void remove() {
        throw new UnsupportedOperationException("#remove() not implemented.");
    }

    private void serializeRecord(Record record) throws IOException{
        if (m_basePath != null){
            File marc = Main.getPath(m_basePath, "marc", counter, m_limit, "mrc");
            MarcStreamWriter streamWriter = new MarcStreamWriter(new BufferedOutputStream(new FileOutputStream(marc)));
            m_logProxy.log(Main.WRITE_MARC);
            streamWriter.write(record);
            m_logProxy.log(Main.WRITE);
            streamWriter.close();
        }
    }


}
