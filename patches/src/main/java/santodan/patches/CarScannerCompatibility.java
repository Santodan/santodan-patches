package santodan.patches;

import app.morphe.patcher.patch.ApkFileType;
import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.Compatibility;
import java.util.List;

/** Exact inspected target; do not extend to older inputs. */
public final class CarScannerCompatibility {
    public static final String PACKAGE = "com.ovz.carscanner";
    public static final String VERSION = "2.1.50";
    private CarScannerCompatibility() {}

    public static Compatibility create() {
        return new Compatibility(PACKAGE, "Car Scanner ELM OBD2", null,
            ApkFileType.APK, null, null,
            List.of(new AppTarget(VERSION, false, 21), new AppTarget("2.2.21", false, 23)), false);
    }
}
