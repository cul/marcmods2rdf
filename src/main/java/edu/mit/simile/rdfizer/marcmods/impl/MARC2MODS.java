package edu.mit.simile.rdfizer.marcmods.impl;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;


public class MARC2MODS extends AbstractTransform {

    static final String MARCXML_2_MODSXML = "MARC21slim2MODS3.xsl";
    static final String WEB_MARCXML_2_MODSXML = "http://www.loc.gov/standards/mods/v3/MARC21slim2MODS3.xsl";

    private final Templates m_templates;

    public MARC2MODS(File transformationDir) throws TransformerConfigurationException {
        this(null, transformationDir);
    }

    public MARC2MODS(AbstractTransform sourceTransform, File transformationDir) throws TransformerConfigurationException {
        super(sourceTransform);
        m_templates = getTemplates(transformationDir, MARCXML_2_MODSXML, WEB_MARCXML_2_MODSXML);
    }

    @Override
    protected void doTransform(Source input, Result output)
            throws TransformerException {
        Transformer t = m_templates.newTransformer();
        t.transform(input, output);
    }

}
