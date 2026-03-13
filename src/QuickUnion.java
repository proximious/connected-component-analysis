public class QuickUnion {

    // Parent links for each element
    private int[] id;

    // Number of connected components remaining
    private int components;

    // Size of the tree for each root (used for weighted union)
    private int[] sz;

    // Initializes union find structure with N isolated elements
    public QuickUnion(int N) {
        this.id = new int[N];
        this.sz = new int[N];

        // Each element starts as its own root
        for (int i = 0; i < N; i++) {
            this.id[i] = i;
            this.sz[i] = 1;
        }

        this.components = N;
    }

    public int getSize() {
        return this.id.length;
    }

    // Finds the root of element i with path compression
    // Path compression flattens the tree to speed up future operations
    private int root(int i) {
        while (i != this.id[i]) {
            id[i] = id[id[i]]; // point node to its grandparent
            i = this.id[i];
        }
        return i;
    }

    // Checks if two elements belong to the same component
    public boolean connected(int p, int q) {
        return this.root(p) == this.root(q);
    }

    // Connects the components containing p and q using weighted union
    // The smaller tree is attached under the larger one
    public void union(int p, int q) {
        int i = this.root(p);
        int j = this.root(q);

        if (i == j) {
            return;
        }

        if (sz[i] < sz[j]) {
            id[i] = j;
            sz[j] += sz[i];
        } else {
            id[j] = i;
            sz[i] += sz[j];
        }

        this.components -= 1;
    }

    // Returns the root identifier for element p
    public int find(int p) {
        return this.root(p);
    }

    // Returns the number of remaining components
    public int count() {
        return this.components;
    }
}