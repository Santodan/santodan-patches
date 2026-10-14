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
            "Makes the managed premium-status getter return true in Car Scanner 2.1.50.",
            false, builder -> {
                builder.compatibleWith(CarScannerCompatibility.create());
                builder.execute(context -> {
                    if (!CarScannerCompatibility.PACKAGE.equals(context.getPackageMetadata().getPackageName())
                        || !CarScannerCompatibility.VERSION.equals(context.getPackageMetadata().getVersionName()))
                        throw new IllegalStateException("Expected Car Scanner 2.1.50");
                    try {
                        Path store = context.get(CarScannerAssemblyStore.ENTRY, false).toPath();
                        byte[] rewritten = CarScannerAssemblyStore.patch(Files.readAllBytes(store),
                            Files.readAllBytes(context.get(CarScannerAssemblyStore.AOT_ENTRY, false).toPath()));
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
