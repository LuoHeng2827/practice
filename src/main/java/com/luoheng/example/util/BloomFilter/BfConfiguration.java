package com.luoheng.example.util.BloomFilter;

/**
 * 
 * @author Administrator
 *
 */
public class BfConfiguration {
    private String host;
    private int port;
    private String password;
    private int hashCount;
    private double negativeRate;
    private int dataSize;
    private int bitLength;
    /**
     * @param host redis主机ip
     * @param port redis端口
     * 直接指定Bit的长度
     * */
    public BfConfiguration(String host, int port, int bitLength) {
        this.host = host;
        this.port = port;
        this.bitLength = bitLength;
        this.hashCount= 7;
        if (bitLength < 0){
            throw  new RuntimeException("用于去重的位信息长度不能为0");
        }
    }
    /**
     * @param host redis主机ip
     * @param port redis端口
     * @param hashCount 需要使用的hash算法的数量,最多15个,最少1个
     * @param negativeRate 误判率
     * @param dataSize 预估要判重的数量个数
     * 其中hashNumber,negativeRate,estDataNumber会影响计算出的需要bit的个数（长度）
     * */
    public BfConfiguration(String host,int port,int hashCount,double negativeRate,int dataSize) {
        this.host = host;
        this.port = port;
        this.hashCount=hashCount;
        this.negativeRate=negativeRate;
        this.dataSize=dataSize;
        this.bitLength = computeBitLength(hashCount,negativeRate,dataSize);
    }
    /**
     * @param host redis主机ip
     * @param port redis端口
     * @param hashCount 需要使用的hash算法的数量,最多15个,最少1个
     * @param negativeRate 误判率
     * @param dataSize 预估要判重的数量个数
     * 其中hashNumber,negativeRate,estDataNumber会影响计算出的需要bit的个数（长度）
     * */
    public BfConfiguration(String host,int port,String password,int hashCount,double negativeRate,int dataSize) {
        this.host = host;
        this.port = port;
        this.password=password;
        this.hashCount=hashCount;
        this.negativeRate=negativeRate;
        this.dataSize=dataSize;
        this.bitLength = computeBitLength(hashCount,negativeRate,dataSize);
    }
    private int computeBitLength(int hashNumber, double falsePositiveRate, int estDatanumber){
        if (hashNumber > 15 || hashNumber <0){
            throw new BfborException("哈希算法的数量应在 1~15 之间");
        }
        if (falsePositiveRate >=1 || falsePositiveRate <=0){
            throw new BfborException("重复率在 0~1 之间");
        }
        if (estDatanumber < 0){
            throw new BfborException("预估数据长度不能小于0");
        }
        double up = -1 * hashNumber * estDatanumber;
        double down = Math.log(1 - Math.pow(falsePositiveRate,1.0/hashNumber));
        int result = (int)(up / down);
        return result;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getHashCount() {
        return hashCount;
    }

    public void setHashCount(int hashCount) {
        this.hashCount=hashCount;
    }

    public double getNegativeRate() {
        return negativeRate;
    }

    public void setNegativeRate(double negativeRate) {
        this.negativeRate=negativeRate;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize=dataSize;
    }

    public int getBitLength() {
        return bitLength;
    }

    public void setBitLength(int bitLength) {
        this.bitLength = bitLength;
    }

    @Override
    public String toString() {
        return "BfConfiguration{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", hashCount=" +hashCount+
                ", negativeRate=" +negativeRate+
                ", dataSize=" +dataSize+
                ", bitLength=" + bitLength +
                '}';
    }
}
