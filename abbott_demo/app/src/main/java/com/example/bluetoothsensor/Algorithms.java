package com.example.bluetoothsensor; 
  
import java.util.List;

public class Algorithms  
{ 
  
    /* 
     * Parameters for the swallow detection algorithm 
     */
    final static int WINDOW_SIZE = 10; 
      
    /* 
     * This is adjustable by the seekbar 
     */
    static double THRESHOLD = 5; 
  
      
    /* 
     * How long to wait after detecting a swallow before detecting another 
     */
    final static int DISABLE_LENGTH = 50;  
    static int DISABLE_COUNTER = -50; 
      
      
    /* 
     * How many swallows we have detected 
     */
    static int SWALLOW_COUNT = 0; 
      
  
      
      
     /* 
     * Estimate if they are wearing the necklace 
     */
    static int ConsecutiveZero = 0; 
    static double LastReading = 0; 
  
      
      
      
      
    /* 
     * This function is called every time BT data is received. 
     * It goes through the array structures and looks for swallows. 
     * The algorithm is implemented here 
     */
//
//    public static void DetectSwallows()
//    {
//        double mean, stddev;
//
//        List<SensorData>  VibrationDataList = MainUI.VibrationDataList;
//        List<Double> VibrationDeviationList = MainUI.VibrationDeviationList;
//
//        mean = 0;
//        stddev = 0;
//
//        /*
//         * Not enough data has accumulated to detect anything
//         */
//        if(VibrationDataList.size() < WINDOW_SIZE)
//            return;
//
//
//        /*
//         * Here we calculate the average value in a given window
//         */
//        int sum = 0;
//        int VibrationDataSize = VibrationDataList.size();
//
//        for(int i = VibrationDataSize - WINDOW_SIZE; i < VibrationDataSize; i++)
//        {
//            try
//            {
//                sum = sum + Integer.parseInt(VibrationDataList.get(i).iValue);
//            }
//            catch(NumberFormatException exc)
//            {
//                continue;
//            }
//        }
//
//        mean = sum / WINDOW_SIZE;
//        mean = mean / 100;
//        double sum_variance = 0;
//
//
//
//        /*
//         * Here, we calculate the standard deviation of each point within a detection window.
//         * This is an important feature used to detect swallows.
//         */
//        for(int i = VibrationDataSize - WINDOW_SIZE; i < VibrationDataSize; i++)
//        {
//            SensorData current = VibrationDataList.get(i);
//            try
//            {
//                int val = Integer.parseInt(current.iValue);
//                double valD = (double)val;
//                valD = valD / 100;
//
//                double diff = Math.abs(mean - valD);
//                sum_variance = sum_variance + diff*diff;
//
//            }
//            catch(NumberFormatException exc)
//            {
//                continue;
//            }
//        }
//
//        stddev = Math.floor(100*Math.sqrt(sum_variance))/100;
//
//        /*
//         * We can prevent the array from getting too big this way....
//         * We only need the last 50 samples anyway
//         */
//        if(VibrationDeviationList.size() > 50)
//        {
//            VibrationDeviationList.remove(0);
//        }
//
//        VibrationDeviationList.add(Double.valueOf(stddev));
//
//
//
//        DISABLE_COUNTER++;
//
//        if(stddev > THRESHOLD)
//        {
//            if(DISABLE_COUNTER >= DISABLE_LENGTH)
//            {
//                /*
//                 * Swallow detected!
//                 */
//                DISABLE_COUNTER = 0;
//                if(ConnectFragment.RegisterSwallows == true)
//                {
//                    SWALLOW_COUNT++;
//                }
//            }
//        }
//
//        ConnectFragment.SetStdDev(stddev);
//        ConnectFragment.MainProgressBar.setProgress((SWALLOW_COUNT*10)%100);
//
//        DetectNecklaceStatus(stddev);
//    }
//
//
//    /*
//     * This function detects whether or not the necklace is being worn.
//     * We simply look for long periods with no variation in sensor data.
//     */
//    public static void DetectNecklaceStatus(double val)
//    {
//        if(val < .15)
//        {
//            ConsecutiveZero++;
//        }
//        else
//        {
//            ConsecutiveZero = 0;
//            ConnectFragment.LabelOnOff.setText("");
//        }
//
//        LastReading = val;
//
//        if(ConsecutiveZero > 100)
//        {
//        	ConnectFragment.LabelOnOff.setText("Necklace Off");
//        }
//    }
} 