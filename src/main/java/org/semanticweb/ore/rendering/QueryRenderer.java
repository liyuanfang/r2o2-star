package org.semanticweb.ore.rendering;

import java.io.IOException;

import org.semanticweb.ore.querying.Query;

public interface QueryRenderer {

	public boolean renderQuery(Query query) throws IOException;

}
