package software.santodan.extension.nuviomerged;

/** Verified version-specific provider accessors; erased Flow types carry different values. */
public final class NuvioProviderLayout {
    public final String providerInterface;
    public final String allProgressMethod;
    public final String siblingsMethod;
    public final String localWatchedField = "i";
    public final String refreshMethod;
    public final String progressWriteMethod;
    public final String snapshotReadyMethod;
    public final String itemProgressMethod;
    private NuvioProviderLayout() {
        progressWriteMethod = "r";
        snapshotReadyMethod = "p";
        itemProgressMethod = "o";
        providerInterface = "q9.z";
        allProgressMethod = "q";
        siblingsMethod = "w";
        refreshMethod = "v";
    }
    public static NuvioProviderLayout forRepository(String repositoryClass) {
        if (!"ba.ze".equals(repositoryClass))
            throw new IllegalStateException("Unsupported Nuvio repository: " + repositoryClass);
        return new NuvioProviderLayout();
    }
}
