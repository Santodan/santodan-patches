package santodan.patches;

import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.patch.RawResourcePatch;
import kotlin.Unit;
import java.nio.file.Files;
import java.nio.file.Path;

/** Rewrites the managed purchase-status getter in the exact inspected APK. */
public final class CarScannerManagedPatch {
    private CarScannerManagedPatch() {}

    public static RawResourcePatch getCarScannerManagedPatch() {
        return PatchKt.rawResourcePatch("Car Scanner - Premium status",
            "Enables premium status in Car Scanner 2.1.50 and 2.2.21; preserves profile decoding and ECU initialization and prevents the repackaging exit in 2.2.21.",
            false, builder -> {
                builder.compatibleWith(CarScannerCompatibility.create());
                builder.execute(context -> {
                    if (!CarScannerCompatibility.PACKAGE.equals(context.getPackageMetadata().getPackageName())
                        || !(CarScannerCompatibility.VERSION.equals(context.getPackageMetadata().getVersionName())
                            || "2.2.21".equals(context.getPackageMetadata().getVersionName())))
                        throw new IllegalStateException("Expected Car Scanner 2.1.50 or 2.2.21");
                    try {
                        if ("2.2.21".equals(context.getPackageMetadata().getVersionName())) {
                            // Patcher 2 extracts archive entries on demand; request extraction before reading.
                            Path store = context.get(CarScanner221.ENTRY, true).toPath();
                            Path aot = context.get(CarScanner221.AOT_ENTRY, true).toPath();
                            Path guard = context.get(CarScanner221.GUARD_ENTRY, true).toPath();
                            Path decoder = context.get(CarScanner221.DECODER_ENTRY, true).toPath();
                            var rewritten = CarScanner221.patch(Files.readAllBytes(store),
                                Files.readAllBytes(aot), Files.readAllBytes(guard));
                            byte[] rewrittenDecoder = CarScanner221.patchDecoder(Files.readAllBytes(decoder));
                            Files.write(store, rewritten.store());
                            Files.write(aot, rewritten.aot());
                            Files.write(guard, rewritten.guard());
                            Files.write(decoder, rewrittenDecoder);
                            return Unit.INSTANCE;
                        }
                        Path store = context.get(CarScannerAssemblyStore.ENTRY, true).toPath();
                        byte[] rewritten = CarScannerAssemblyStore.patch(Files.readAllBytes(store),
                            Files.readAllBytes(context.get(CarScannerAssemblyStore.AOT_ENTRY, true).toPath()));
                        Files.write(store, rewritten);
                    } catch (java.io.IOException exception) {
                        throw new IllegalStateException("Could not rewrite Car Scanner assembly store", exception);
                    }
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
    }
}
