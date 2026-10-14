package santodan.patches;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipFile;

/** Real-input verifier, including all unaffected assembly blocks and failure paths. */
public final class VerifyCarScannerManaged {
    public static void main(String[] args) throws Exception {
        byte[] store, aot;
        try (ZipFile apk = new ZipFile(args[0])) {
            store = apk.getInputStream(apk.getEntry(CarScannerAssemblyStore.ENTRY)).readAllBytes();
            aot = apk.getInputStream(apk.getEntry(CarScannerAssemblyStore.AOT_ENTRY)).readAllBytes();
        }
        byte[] patched = CarScannerAssemblyStore.patch(store, aot);
        verify(store, patched);
        reject(patched, aot);
        byte[] damaged = store.clone();
        damaged[16384 + 4] ^= 1;
        reject(damaged, aot);
        byte[] differentAot = aot.clone();
        differentAot[0x87dfc] ^= 1;
        reject(store, differentAot);
        if (args.length > 1) Files.write(Path.of(args[1]), patched);
        System.out.println("PASS: 208 assembly images preserved except target IL; ELF reload and unsupported/already-modified rejection");
    }

    public static void verify(byte[] original, byte[] patched) {
        int descriptors = 16384 + 20 + 4992;
        for (int i = 0; i < 208; i++) {
            int d = descriptors + i * 28;
            int oldStart = 16384 + CarScannerAssemblyStore.integer(original, d + 4);
            int newStart = 16384 + CarScannerAssemblyStore.integer(patched, d + 4);
            int oldSize = CarScannerAssemblyStore.integer(original, d + 8);
            int newSize = CarScannerAssemblyStore.integer(patched, d + 8);
            if (i != 121) {
                if (!Arrays.equals(Arrays.copyOfRange(original, oldStart, oldStart + oldSize),
                    Arrays.copyOfRange(patched, newStart, newStart + newSize)))
                    throw new AssertionError("Unrelated image changed: " + i);
            } else {
                byte[] before = CarScannerAssemblyStore.decodeLz4(original, oldStart + 12,
                    oldStart + oldSize, CarScannerAssemblyStore.integer(original, oldStart + 8));
                byte[] after = Arrays.copyOfRange(patched, newStart, newStart + newSize);
                if (after.length != before.length || after[0x9be64] != 0x17 || after[0x9be65] != 0x2a)
                    throw new AssertionError("Getter not rewritten");
                for (int j = 0; j < before.length; j++) {
                    if (j >= 0x9be64 && j < 0x9be64 + 142) continue;
                    if (before[j] != after[j]) throw new AssertionError("Unrelated managed bytes changed");
                }
                for (int j = 0x9be66; j < 0x9be64 + 142; j++)
                    if (after[j] != 0) throw new AssertionError("Unexpected unreachable IL");
            }
        }
        var oldElf = java.nio.ByteBuffer.wrap(original).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        var newElf = java.nio.ByteBuffer.wrap(patched).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int oldHeaders = Math.toIntExact(oldElf.getLong(40));
        int newHeaders = Math.toIntExact(newElf.getLong(40));
        int delta = patched.length - original.length;
        if (newHeaders != oldHeaders + delta) throw new AssertionError("ELF section table not relocated");
        for (int i = 0; i < Short.toUnsignedInt(oldElf.getShort(60)); i++) {
            int o = oldHeaders + i * 64, n = newHeaders + i * 64;
            long offset = newElf.getLong(n + 24), length = newElf.getLong(n + 32);
            if (newElf.getInt(n + 4) != 8 && offset + length > patched.length)
                throw new AssertionError("ELF section out of bounds");
            if (oldElf.getLong(o + 24) == 16384 && length != oldElf.getLong(o + 32) + delta)
                throw new AssertionError("Payload length not updated");
        }
        // The allocated ELF segments and all name-hash index entries are untouched.
        for (int i = 0; i < descriptors; i++) {
            if (i >= 40 && i < 48) continue;
            if (original[i] != patched[i]) throw new AssertionError("ELF prefix or index changed");
        }
    }

    private static void reject(byte[] store, byte[] aot) {
        try { CarScannerAssemblyStore.patch(store, aot); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("Unsupported input accepted");
    }
}
