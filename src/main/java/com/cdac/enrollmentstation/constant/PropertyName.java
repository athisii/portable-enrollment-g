package com.cdac.enrollmentstation.constant;

/**
 * @author athisii, CDAC
 * Created on 17/12/22
 */

// A utility class for mapping properties names in /etc/enrollment-app.properties file.
public class PropertyName {
    //Suppress default constructor for noninstantiability
    private PropertyName() {
        throw new AssertionError("The PropertyName fields should be accessed statically");
    }

    //DEFAULT_PROPERTY_FILE = "/etc/enrollment-app.properties"

    public static final String LOG_FILE = "log.file";
    public static final String INITIALIZE = "initialize";
    public static final String WAIT_FOR_CONNECT = "wait.for.connect";
    public static final String SELECT_APP = "select.app";
    public static final String READ_DATA = "read.data";
    public static final String STORE_DATA = "store.data";
    public static final String VERIFY_CERT = "verify.cert";
    public static final String PKI_AUTH = "pki.auth";
    public static final String WAIT_FOR_REMOVAL = "wait.for.removal";
    public static final String DE_INITIALIZE = "de.initialize";
    public static final String LIST_OF_READERS = "list.of.readers";
    public static final String CERT_FILE = "cert.file";
    public static final String PASSPHRASE = "passphrase";
    public static final String URL_DATA = "url.data";
    public static final String FP_QUALITY = "fp.quality";
    public static final String ADMIN_PASSWD = "admin.passwd";
    public static final String INPUT_FILE = "input.file";
    public static final String CAP_COMMAND = "cap.command";
    public static final String WEBCAM_COMMAND = "webcam.command";
    public static final String OUTPUT_FILE = "output.file";
    public static final String COMPRESS_FILE = "compress.file";
    public static final String IMPORT_JSON_FILE = "import.json.file";
    public static final String EXPORT_FOLDER = "export.folder";
    public static final String SAVE_ENROLLMENT = "save.enrollment";
    public static final String IRIS_FILE = "iris.file";
    public static final String SLAP_SCAN_FILE = "slap.scan.file";
    public static final String CAMERA_FILE_PATH = "camera.file.path";
    public static final String BARCODE_FILE_PATH = "barcode.file.path";
    public static final String SLAP_LICENSE = "slap.license";
    public static final String DOMAIN = "domain";
    public static final String LDAP_URL = "ldap.url";
    public static final String SERIAL_PORT_FILE = "serial.port.file";
    public static final String IMPORT_JSON_FOLDER = "import.json.folder";
    public static final String CUR_PES_ID = "cur.pes.id";
    public static final String ENC_EXPORT_FOLDER = "enc.export.folder";
    public static final String CAMERA_ID = "camera.id";
    public static final String PHOTO_CAPTURE_IMG = "photo.capture.img";
    public static final String SUB_FILE = "sub.file";
    public static final String ENV = "env";
    public static final String LIC_IENGINE = "lic.iengine";
    public static final String MAFIS_API_URL = "mafis.api.url";
    public static final String ENROLLMENT_STATION_ID = "enrollment.station.id";
    public static final String ENROLLMENT_STATION_UNIT_ID = "enrollment.station.unit.id";
    public static final String FINGERPRINT_LIVENESS_MAX = "fingerprint.liveness.max";
    public static final String FINGERPRINT_LIVENESS_MIN = "fingerprint.liveness.min";
    public static final String FINGERPRINT_LIVENESS_VALUE = "fingerprint.liveness.value";


}
