package edu.mit.simile.rdfizer.marcmods;

import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;


public interface Transform {
    public void transform(Source input, Result output) throws IOException, TransformerException;
}
