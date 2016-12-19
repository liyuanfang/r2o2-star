package ore.rendering;

import ore.querying.Query;

import java.io.IOException;

public interface QueryRenderer {

	public boolean renderQuery(Query query) throws IOException;

}
