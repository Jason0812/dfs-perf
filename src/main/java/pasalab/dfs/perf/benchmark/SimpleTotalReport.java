package pasalab.dfs.perf.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pasalab.dfs.perf.basic.PerfTaskContext;
import pasalab.dfs.perf.basic.PerfTotalReport;

public class SimpleTotalReport extends PerfTotalReport {
  protected String mFailedSlaves = "";
  protected int mFailedTasks = 0;
  protected long mId = Long.MAX_VALUE;
  protected int mSlavesNum;

  protected List<String> mSlaves;
  protected Map<String, String> mConf;
  protected List<Map<String, List<Double>>> mStatistics;

  @Override
  public void initialFromTaskContexts(PerfTaskContext[] taskContexts) throws IOException {
    mSlavesNum = taskContexts.length;
    mSlaves = new ArrayList<String>(mSlavesNum);
    mConf = ((SimpleTaskContext) taskContexts[0]).getConf();
    mStatistics = new ArrayList<Map<String, List<Double>>>(mSlavesNum);

    for (PerfTaskContext taskContext : taskContexts) {
      SimpleTaskContext context = (SimpleTaskContext) taskContext;
      mSlaves.add(context.getId() + "@" + context.getNodeName());
      mStatistics.add(context.getAdditiveStatistics());

      if (context.getStartTimeMs() < mId) {
        mId = context.getStartTimeMs();
      }
      if (!context.getSuccess()) {
        mFailedTasks ++;
        mFailedSlaves += context.getId() + "@" + context.getNodeName() + " ";
      }
    }
  }

  private String generateSlaveDetails(int slaveIndex) {
    StringBuffer sbSlaveDetail = new StringBuffer();
    String slaveName = mSlaves.get(slaveIndex);
    Map<String, List<Double>> statistic = mStatistics.get(slaveIndex);
    for (Map.Entry<String, List<Double>> entry : statistic.entrySet()) {
      sbSlaveDetail.append(slaveName).append("'s ").append(entry.getKey())
          .append(" for each threads:\n\t");
      for (Double d : entry.getValue()) {
        sbSlaveDetail.append("[ ").append(d).append(" ]");
      }
      sbSlaveDetail.append("\n");
    }
    sbSlaveDetail.append("\n");
    return sbSlaveDetail.toString();
  }

  private String generateTaskConf() {
    StringBuffer sbReadConf = new StringBuffer();
    for (Map.Entry<String, String> entry : mConf.entrySet()) {
      sbReadConf.append(entry.getKey()).append("\t").append(entry.getValue()).append("\n");
    }
    return sbReadConf.toString();
  }

  private String generateStatistics() {
    StringBuffer sbStatistics = new StringBuffer("SlaveName");
    Map<String, List<Double>> sample = mStatistics.get(0);
    int size = sample.size();
    List<String> names = new ArrayList<String>(size);
    List<Double> totals = new ArrayList<Double>(size);
    for (String name : sample.keySet()) {
      names.add(name);
      totals.add(0.0);
      sbStatistics.append("\t").append(name);
    }
    sbStatistics.append("\n");
    for (int i = 0; i < mStatistics.size(); i ++) {
      sbStatistics.append(mSlaves.get(i));
      Map<String, List<Double>> statistic = mStatistics.get(i);
      for (int t = 0; t < size; t ++) {
        List<Double> threadDetails = statistic.get(names.get(t));
        double sum = 0;
        for (Double d : threadDetails) {
          sum += d;
        }
        sbStatistics.append("\t").append(sum);
        totals.set(t, totals.get(t) + sum);
      }
      sbStatistics.append("\n");
    }
    sbStatistics.append("Total");
    for (Double total : totals) {
      sbStatistics.append("\t").append(total);
    }
    sbStatistics.append("\n");
    return sbStatistics.toString();
  }

  @Override
  public void writeToFile(File file) throws IOException {
    BufferedWriter fout = new BufferedWriter(new FileWriter(file));
    fout.write(mTestCase + " Test - ID : " + mId + "\n");
    if (mFailedTasks == 0) {
      fout.write("Finished Successfully\n");
    } else {
      fout.write("Failed: " + mFailedTasks + " slaves failed ( " + mFailedSlaves + ")\n");
    }
    fout.write("********** Task Configurations **********\n");
    fout.write(generateTaskConf());
    fout.write("********** Statistics **********\n");
    fout.write(generateStatistics());
    fout.write("********** Slave Details **********\n");
    for (int i = 0; i < mSlavesNum; i ++) {
      fout.write(generateSlaveDetails(i));
    }
    fout.close();
  }

}
