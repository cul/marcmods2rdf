package edu.mit.simile.rdfizer.marcmods.impl;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import edu.mit.simile.rdfizer.marcmods.Transform;


public abstract class AbstractTransform implements Transform {
    private final AbstractTransform m_sourceTransform;
    private static SAXTransformerFactory FACTORY;
    public AbstractTransform(AbstractTransform sourceTransform){
       m_sourceTransform = sourceTransform;
    }

    protected static synchronized Templates getTemplates(File transformDir, String defaultFilename, String webSource)
            throws TransformerConfigurationException {
        if (FACTORY == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            if (!factory.getFeature(SAXTransformerFactory.FEATURE)) {
                throw new UnsupportedOperationException("SAXTransformerFactory is not supported");
            }
            FACTORY = (SAXTransformerFactory) factory;
        }
        Source source;
        if (transformDir != null) {
            if (transformDir.isDirectory()){
                File tSource = new File(transformDir, defaultFilename);
                source = (tSource.exists()) ? new StreamSource(tSource) : new StreamSource(webSource);
            } else {
                source = new StreamSource(transformDir);
            }
        } else {
            source = new StreamSource(webSource);
        }
        return FACTORY.newTemplates(source);
    }

    public void transform(Source input, Result output) throws TransformerException, IOException {
        if (m_sourceTransform != null){
            doTransform(m_sourceTransform.chain(input), output);
        } else {
            doTransform(input, output);
        }
    }

    protected abstract void doTransform(Source input, Result output) throws TransformerException;

    public Source chain(Source input) throws IOException, TransformerException {
        DOMResult output = new DOMResult();
        transform(input, output);
        return new DOMSource(output.getNode());
    }
}
