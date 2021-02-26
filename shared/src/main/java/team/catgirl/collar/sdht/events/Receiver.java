package team.catgirl.collar.sdht.events;

import team.catgirl.collar.sdht.memory.InMemoryDistributedHashTable;

public final class Receiver {
    private final InMemoryDistributedHashTable hashTable;

    public Receiver(InMemoryDistributedHashTable hashTable) {
        this.hashTable = hashTable;
    }
}
