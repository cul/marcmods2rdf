package edu.mit.simile.rdfizer.marcmods.impl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;


public class DOM2XML extends AbstractTransform {
    private final TransformerFactory m_factory;

    public DOM2XML(){
        this(null);
    }
    public DOM2XML(AbstractTransform sourceTransform) {
        super(sourceTransform);
        m_factory = TransformerFactory.newInstance();
    }

    @Override
    protected void doTransform(Source input, Result output)
            throws TransformerException {
        Transformer transformer = m_factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(input, output);
    }

}
