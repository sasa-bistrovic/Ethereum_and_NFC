package co.coinfinity.infineonandroidapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import co.coinfinity.infineonandroidapp.infineon.NfcUtils;
import co.coinfinity.infineonandroidapp.infineon.exceptions.NfcCardException;
import co.coinfinity.infineonandroidapp.utils.ByteUtils;
import co.coinfinity.infineonandroidapp.utils.IsoTagWrapper;
import co.coinfinity.infineonandroidapp.utils.UiUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static android.app.PendingIntent.getActivity;
import static co.coinfinity.AppConstants.TAG;
import static co.coinfinity.infineonandroidapp.utils.UiUtils.showToast;

import androidx.appcompat.app.AppCompatActivity;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

/**
 * Activity class used for generating from seed functionality.
 */
public class GenerateFromSeedActivity extends AppCompatActivity {

    @BindView(R.id.seed)
    TextView seed;
    @BindView(R.id.pin)
    TextView pinTxt;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private String pubKeyString;
    private String privateKeyString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_from_seed);
        ButterKnife.bind(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        pendingIntent = getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);
    }

    /**
     * Called by Android systems whenever a new Intent is received. NFC tags are also
     * delivered via an Intent.
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        resolveIntent(intent);
    }

    /* Will be called after card was hold to back of device.
     *
     * @param intent includes nfc extras
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void resolveIntent(Intent intent) {
        // Only handle NFC intents
        if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) == null) {
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        UiUtils.logTagInfo(tag);
        MifareUltralight isoDep = MifareUltralight.get(tag);
        if (isoDep == null) {
            showToast(getString(R.string.wrong_card), this);
            return;
        }

        try {
            isoDep.connect();

                if (seed.getText().toString().length()==64) {

                    Credentials credentials = Credentials.create(seed.getText().toString());

                    pubKeyString = credentials.getEcKeyPair().getPublicKey().toString(16);

                    privateKeyString = credentials.getEcKeyPair().getPrivateKey().toString(16);

                    String sasakey = pubKeyString;

                    Integer sasaindex = ((int) pubKeyString.length() / 8) + 1;

                    for (int i = 0; i <= sasaindex * 8; i++) {
                        if (sasakey.length() < i) {
                            sasakey = sasakey + "0";
                        }
                    }

                    //showToast(" " + sasaindex + " " + sasakey.length(), this);

                    for (int i = 1; i <= sasakey.length(); i++) {
                        if ((i % 8) == 0) {
                            if (i == sasakey.length()) {
                                isoDep.writePage(((int) i / 8) + 10, hexStringToByteArray(sasakey.substring(i - 8, sasakey.length())));
                                break;
                            } else {
                                isoDep.writePage(((int) i / 8) + 10, hexStringToByteArray(sasakey.substring(i - 8, i)));
                            }
                        }
                    }

                    sasakey = privateKeyString;

                    sasaindex = ((int) privateKeyString.length() / 8) + 1;

                    for (int i = 0; i <= sasaindex * 8; i++) {
                        if (sasakey.length() < i) {
                            sasakey = sasakey + "0";
                        }
                    }

                    //showToast(" " + sasaindex + " " + sasakey.length(), this);

                    for (int i = 1; i <= sasakey.length(); i++) {
                        if ((i % 8) == 0) {
                            if (i == sasakey.length()) {
                                isoDep.writePage(((int) i / 8) + 28, hexStringToByteArray(sasakey.substring(i - 8, sasakey.length())));
                                break;
                            } else {
                                isoDep.writePage(((int) i / 8) + 28, hexStringToByteArray(sasakey.substring(i - 8, i)));
                            }
                        }
                    }
                }
            //}



            //showToast("A : "+sasakey.length(), this);
            /*
            if (NfcUtils.generateKeyFromSeed(IsoTagWrapper.of(isoDep), ByteUtils.fromHexString(seed.getText().toString()), pinTxt.getText().toString().getBytes(StandardCharsets.UTF_8))) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.generate_from_seed)
                        .setMessage(String.format(getString(R.string.generate_from_seed_message), seed.getText()))
                        .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                        .show();
            }

             */
            isoDep.close();
        } catch (IOException e) {
            showToast(e.getMessage(), this);
            Log.e(TAG, "Exception while generating key from seed", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }
}
