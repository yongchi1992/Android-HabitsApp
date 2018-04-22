package edu.northwestern.mhealth395.neckmonitor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by William on 2/6/2016
 */
public class NecklaceEvent {

    private float float0;
    private float float2;
    private float float3;
    private float float4;
    private float float1;
    private long  timeStamp;

    private final String TAG = "NecklaceEvent";

    public NecklaceEvent (byte[] bytes) {
        timeStamp = System.currentTimeMillis();
        if (bytes.length == 20) {
            int proximity = ((int)bytes[0] & 0x0ff) + ((int)bytes[1] & 0x0ff) << 8;
            int ambient = ((int)bytes[2] & 0x0ff) + ((int)bytes[3] & 0x0ff) << 8;
            int calibration = ((int)bytes[12] & 0x0ff) + ((int)bytes[13] & 0x0ff) << 8;

            float0 = (float)proximity;
            float1 = (float)ambient;
            float2 = ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.nativeOrder()).getFloat();
            float3 = ByteBuffer.wrap(bytes, 8, 4).order(ByteOrder.nativeOrder()).getFloat();

            float3 = float0;

            float4 = (float)calibration;

        } else {
//            Log.e(TAG, "Wrong number of bytes in constrctor array");
            float0 = 0;
            float2 = 0;
            float3 = 0;
            float4 = 0;
            float1 = 0;
        }
    }

    public float getFloat0() { return float0; }
    public float getFloat2() { return float2; }
    public float getFloat3() { return float3; }
    public float getFloat4()  { return float4;  }
    public float getFloat1(){ return float1;}
    public long  getTimeStamp() { return timeStamp; }


}
