package software.santodan.extension.nuviomerged;

/** Verified beta.6 provider accessors; erased Flow types still carry different values. */
public final class NuvioProviderLayout {
    public final String providerInterface = "o9.z";
    public final String allProgressMethod = "r";
    public final String siblingsMethod = "w";
    public final String localWatchedField = "i";
    public final String refreshMethod = "o";
    private NuvioProviderLayout() {}
    public static NuvioProviderLayout forRepository(String repositoryClass) {
        if (!"z9.zc".equals(repositoryClass))
            throw new IllegalStateException("Unsupported Nuvio repository: " + repositoryClass);
        return new NuvioProviderLayout();
    }
}
