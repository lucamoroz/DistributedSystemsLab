package dslab.mailbox.storage;

import dslab.protocols.dmtp.Email;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryUserEmails extends ConcurrentHashMap<Integer, Email> implements IUserEmails {

    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public void addEmail(Email email) {
        Integer id = count.incrementAndGet();
        this.put(id, email);
    }

    @Override
    public Set<Entry<Integer, Email>> getUserEmails() {
        return Collections.unmodifiableSet(this.entrySet());
    }

    @Override
    public Email deleteEmail(Integer id) {
        return this.remove(id);
    }

    @Override
    public Email getUserEmail(Integer id) { return this.get(id); }
}
