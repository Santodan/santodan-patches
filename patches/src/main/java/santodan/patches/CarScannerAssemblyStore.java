package santodan.patches;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;

/** Exact original 2.1.50 assembly-store transformation; no DEX mutation. */
public final class CarScannerAssemblyStore {
    public static final String ENTRY = "lib/arm64-v8a/libassemblies.arm64-v8a.blob.so";
    public static final String AOT_ENTRY = "lib/arm64-v8a/libaot-CarScannerMaui.dll.so";
    private static final int PAYLOAD = 16384;
    private static final int DESCRIPTORS = PAYLOAD + 20 + 4992;
    private static final int TARGET = 121;
    private static final int BODY = 0x9be58;
    private CarScannerAssemblyStore() {}

    public static byte[] patch(byte[] store, byte[] aot) {
        requireHash(store, "ab629a592cf8cd9ac886119e543489520d2cbb3391adf96218b590fc50482d4b", "assembly store (unsupported or already modified)");
        requireHash(aot, "904b573b091044d68d120978482f262bfdabe6915870350691c57a4d0886f34d", "AOT image");
        // mono_aot_file_info version 187, method_addresses relocation = 0x8a840.
        // MethodDef index 0x1570-1 has a BL back to method_addresses itself:
        // Mono treats this as an absent compiled body. LLVM code is absent too.
        check(integer(aot, 0x87dfc) == 0x97ffea91, "Unexpected AOT method entry");
        check(integer(store, PAYLOAD) == 0x41424158 && integer(store, PAYLOAD + 4) == 0x80010002,
            "Unexpected XABA format");
        int descriptor = DESCRIPTORS + TARGET * 28;
        int start = PAYLOAD + integer(store, descriptor + 4);
        int size = integer(store, descriptor + 8);
        check(start == 5069940 && size == 24663182 && integer(store, descriptor) == TARGET,
            "Unexpected assembly descriptor");
        check(integer(store, start) == 0x5a4c4158 && integer(store, start + 4) == TARGET,
            "Unexpected XALZ header");
        byte[] image = decodeLz4(store, start + 12, start + size, integer(store, start + 8));
        requireHash(image, "6f4ea3cf3e67ff63628b01261217181f7cdf63a301b3d08aa3b54a752baa505a", "CarScannerMaui.dll");
        // Fat IL header: 12 bytes, no EH sections, max stack 5, code size 142.
        check(integer(image, BODY) == 0x00053013 && integer(image, BODY + 4) == 142
            && integer(image, BODY + 8) == 0, "Unexpected managed method body");
        Arrays.fill(image, BODY + 12, BODY + 12 + 142, (byte) 0);
        image[BODY + 12] = 0x17; // ldc.i4.1
        image[BODY + 13] = 0x2a; // ret

        // Store supports plain PE images as well as XALZ. Keep every other image
        // byte-for-byte; replace this block with MZ and shift later descriptors.
        // Keep the relocated ELF64 section-header table aligned to eight bytes.
        int padding = (size - image.length) & 7;
        int delta = image.length + padding - size;
        byte[] result = new byte[Math.addExact(store.length, delta)];
        System.arraycopy(store, 0, result, 0, start);
        System.arraycopy(image, 0, result, start, image.length);
        System.arraycopy(store, start + size, result, start + image.length + padding, store.length - start - size);
        int end = start + size;
        for (int i = 0; i < 208; i++) {
            int d = DESCRIPTORS + i * 28;
            for (int offsetField : new int[] {4, 12, 20}) {
                int offset = integer(store, d + offsetField);
                if (offset != 0 && PAYLOAD + offset >= end)
                    putInt(result, d + offsetField, Math.addExact(offset, delta));
            }
        }
        putInt(result, descriptor + 8, image.length);
        long oldHeaders = longInteger(store, 40);
        int newHeaders = Math.toIntExact(oldHeaders + delta);
        putLong(result, 40, newHeaders);
        int payloadSections = 0;
        int count = Short.toUnsignedInt(buffer(store).getShort(60));
        for (int i = 0; i < count; i++) {
            int original = Math.toIntExact(oldHeaders) + i * 64;
            int updated = newHeaders + i * 64;
            long offset = longInteger(store, original + 24);
            long length = longInteger(store, original + 32);
            if (offset == PAYLOAD) {
                check(offset + length >= end && integer(store, original + 4) == 1
                    && longInteger(store, original + 8) == 0, "Payload must be non-allocated PROGBITS");
                putLong(result, updated + 32, length + delta);
                payloadSections++;
            } else if (offset >= end) {
                putLong(result, updated + 24, offset + delta);
            }
        }
        check(payloadSections == 1, "Expected one ELF payload section");
        check(result[start] == 'M' && result[start + 1] == 'Z', "Rebuilt image missing");
        return result;
    }

    static byte[] decodeLz4(byte[] source, int position, int end, int expected) {
        check(expected > 0 && expected <= 64 * 1024 * 1024, "Invalid decoded size");
        byte[] output = new byte[expected];
        int written = 0;
        while (position < end && written < expected) {
            int token = source[position++] & 255;
            int literals = token >>> 4;
            if (literals == 15) {
                int extension;
                do {
                    check(position < end, "Truncated LZ4 literal length");
                    extension = source[position++] & 255;
                    literals = Math.addExact(literals, extension);
                } while (extension == 255);
            }
            check(literals <= end - position && literals <= expected - written, "Invalid LZ4 literals");
            System.arraycopy(source, position, output, written, literals);
            position += literals;
            written += literals;
            if (written == expected) break;
            check(position + 2 <= end, "Truncated LZ4 offset");
            int offset = (source[position] & 255) | ((source[position + 1] & 255) << 8);
            position += 2;
            check(offset > 0 && offset <= written, "Invalid LZ4 offset");
            int length = token & 15;
            if (length == 15) {
                int extension;
                do {
                    check(position < end, "Truncated LZ4 match length");
                    extension = source[position++] & 255;
                    length = Math.addExact(length, extension);
                } while (extension == 255);
            }
            length += 4;
            check(length <= expected - written, "LZ4 output overflow");
            for (int j = 0; j < length; j++) output[written + j] = output[written + j - offset];
            written += length;
        }
        check(written == expected && position == end, "LZ4 length mismatch");
        return output;
    }

    static int integer(byte[] bytes, int offset) { return buffer(bytes).getInt(offset); }
    private static long longInteger(byte[] bytes, int offset) { return buffer(bytes).getLong(offset); }
    private static void putInt(byte[] bytes, int offset, int value) { buffer(bytes).putInt(offset, value); }
    private static void putLong(byte[] bytes, int offset, long value) { buffer(bytes).putLong(offset, value); }
    private static ByteBuffer buffer(byte[] bytes) { return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN); }
    private static void requireHash(byte[] bytes, String expected, String label) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder actual = new StringBuilder();
            for (byte value : digest) actual.append(String.format("%02x", value & 255));
            check(expected.equals(actual.toString()), "Unexpected " + label);
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Car Scanner 2.1.50: " + message);
    }
}
