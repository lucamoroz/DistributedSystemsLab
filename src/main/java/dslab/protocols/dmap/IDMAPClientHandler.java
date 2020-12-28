package dslab.protocols.dmap;

import dslab.protocols.dmtp.Email;

import java.io.IOException;
import java.util.Map;

public interface IDMAPClientHandler {

    void init(String username, String password) throws DMAPException, IOException;
    Map<Integer, String[]> list() throws DMAPException, IOException;
    Email show(int id) throws DMAPException, IOException;
    void delete(int id) throws DMAPException, IOException;
    void close() throws DMAPException, IOException;

}
