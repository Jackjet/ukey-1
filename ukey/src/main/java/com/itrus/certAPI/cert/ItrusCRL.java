package com.itrus.certAPI.cert;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import com.itrus.util.DERUtils;
/**
 * <p>Title: ItrusCRL.java</p>
 * <p>Description:</p>
 * @author 牛胜伟
 * @date 2013-3-27 下午6:38:50
 * @version V1.0
 */
public class ItrusCRL {
	private  byte snBuf[];
	private  int snBufPos = 0;
	private  Integer idxBuf[];
	private  Integer idxBufPos = 0;
	private  byte[] notBefor;
	private  byte[] notAfter;
	
	/**
	 * @return the notBefor
	 */
	public byte[] getNotBefor() {
		return notBefor;
	}
	/**
	 * @return the notAfter
	 */
	public byte[] getNotAfter() {
		return notAfter;
	}
	
	public ItrusCRL(){
		
	}
	public ItrusCRL(String crlpath) throws FileNotFoundException{
		InputStream  in=new FileInputStream(crlpath);
		this.loadCRL(in);
	}
	
	public  int buf2int(byte buf[], int len){
		int ret = 0;
		for(int i = 0 ; i < len ; i++ ){
			ret *= 256;
			if(buf[i]>=0)
				ret += buf[i];
			else
				ret += buf[i] + 256;
		}
		return ret;
	}
	private  String BytesToHexString(byte[] bytes, int pos, int len) {
		byte tb;
		char low;
		char high;
		char tmpChar;
		StringBuffer str = new StringBuffer();
		for (int i = pos; i < len; i++) {
			tb = bytes[i];
			tmpChar = (char) ((tb >>> 4) & 0x000f);
			if (tmpChar >= 10) {
				high = (char) (('a' + tmpChar) - 10);
			} else {
				high = (char) ('0' + tmpChar);
			}
			str.append(high);
			tmpChar = (char) (tb & 0x000f);
			if (tmpChar >= 10) {
				low = (char) (('a' + tmpChar) - 10);
			} else {
				low = (char) ('0' + tmpChar);
			}
			str.append(low);
		}
		return str.toString();
	}

	public int findSN(String sn){
		 byte[] findsn1= DERUtils.HexStringToBytes(sn);
		if(findsn1.length<20){
			byte[] tmp = findsn1;
			findsn1 = new byte[20];
			for(int i = 0 ; i < 20 - tmp.length;i++)
				findsn1[i] = 0;
			System.arraycopy(tmp,0,findsn1,20 - tmp.length, tmp.length);
		}else if(findsn1.length == 21 ){
			byte[] tmp = findsn1;
			findsn1 = new byte[20];
			System.arraycopy(tmp,1,findsn1,0,20);
		}
		final byte[] findsn = findsn1;
		Comparator<Integer> comp1 = new Comparator<Integer>(){
			 public int compare(Integer a, Integer b){
				 if(a==-1){
					 Integer temp = a;
					 a = b;
					 b = temp;
				 }
				  int i;
				  for(i = 0 ; i < 20 ; i++ )
				  	if(snBuf[a+i] != findsn[i] )
				  			break;
				  	
				  if(i == 20 )
					  return 0;
				  int aa = snBuf[a+i];
				  int bb = findsn[i];
				  if(aa>=0&&bb>=0){
					  if(aa>bb)
						  return 1;
					  else
						  return -1;
				  }
				  else if(aa>=0&&bb<0)
					  return -1;
				  else if(aa<0&&bb>=0)
					  return 1;
				  else{
					  if(aa>bb)
						  return 1;
					  else
						  return -1;			  
				  }
			  }
		};
		int findIdx = Arrays.binarySearch(idxBuf,0,idxBufPos,-1,comp1);
		return findIdx;
	}
	/**
	 * <p>Description: </p>  
	 * @return     
	 * boolean      
	 */
	public boolean isOnValidPeriod() {
		String str1=new String(this.getNotBefor());
		String str2=new String(this.getNotAfter());
		str1=str1.substring(0, str1.length()-1);
		str2=str2.substring(0, str2.length()-1);
		Date date1=DERUtils.StringToDate(str1, 24);
		Date date2=DERUtils.StringToDate(str2, 24);
		Date date=new Date();
		return!(date.before(date1) || date.after(date2));
	}
	/**
	 * <p>Description: </p>  
	 * @param  fin
	 * @return     
	 * ItrusCRL      
	 */
	public synchronized void loadCRL(InputStream fin){
		try {
			int flen = fin.available();
			snBuf = new byte[flen+128];
			idxBuf = new Integer[(flen+128)/30];
			byte type[] = new byte[1];
			byte len[]= new byte[1];
			byte lenbuf[] = new byte[128];
			byte databuf[] = new byte[1024*100];
			int datalen = 0;
			//sequence 1
			fin.read(type);
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];
			// sequence 2
			fin.read(type);
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];
			// int 3
			fin.read(type);
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];	
			fin.read(databuf,0,datalen);
			
			// sequence 4
			fin.read(type);	
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];
			fin.read(databuf,0,datalen);
			
			// sequence 5
			fin.read(type);
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];
			fin.read(databuf,0,datalen);
			
			// time 6
			fin.read(type);
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];
			fin.read(databuf,0,datalen);
			notBefor = new byte[datalen];
			System.arraycopy(databuf,0,notBefor,0,datalen);
			//System.out.println("datalen = " + datalen);
			
			// time 7
			fin.read(type);
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];
			fin.read(databuf,0,datalen);
			notAfter = new byte[datalen];
			System.arraycopy(databuf,0,notAfter,0,datalen);
			//System.out.println("datalen = " + datalen);
			
			//System.out.println("notBefor = " + new String(notBefor));
			//System.out.println("notAfter = " + new String(notAfter));
			
			// sequence 8
			fin.read(type);
			fin.read(len);
			if(len[0]<0){
				fin.read(lenbuf,0,len[0]+128);
				datalen = buf2int(lenbuf,len[0]+128);
			}
			else
				datalen = len[0];
			
			int revokedSequenceLen = datalen;
			int revokedSequenceIdx = 0;
			
			//System.out.println("loadCRL start " + (new Date().toString()));
			while(revokedSequenceIdx < revokedSequenceLen){
				fin.read(type);
				fin.read(len);
				if(len[0]<0){
					fin.read(lenbuf,0,len[0]+128);
					datalen = buf2int(lenbuf,len[0]+128);
				}
				else
					datalen = len[0];
				
				int curSequenceLen = datalen;
				
				revokedSequenceIdx += curSequenceLen+2;
				
				// 读取序列号
				fin.read(type);
				fin.read(len);
				if(len[0]<0){
					fin.read(lenbuf,0,len[0]+128);
					datalen = buf2int(lenbuf,len[0]+128);
				}
				else
					datalen = len[0];
				fin.read(databuf,0,datalen);
				
				if(datalen != 20){
					String sn = BytesToHexString(databuf,0,datalen);
					//System.out.println(sn +":" + sn.length());
				}
				
				idxBuf[idxBufPos] = snBufPos;
				idxBufPos++;
				if(datalen == 20){
					System.arraycopy(databuf,0,snBuf,snBufPos,20);					
				}
				else if(datalen == 21 ){
					System.arraycopy(databuf,1,snBuf,snBufPos,20);
				}
				else{
					for(int i = 0 ; i < 20 - datalen ; i++)
						snBuf[snBufPos+i] = 0;
					System.arraycopy(databuf,0,snBuf,snBufPos+20-datalen,datalen);
				}
				snBufPos += 20;
				// 跳过后续数据	
				
				fin.read(databuf,0,curSequenceLen - datalen - 2);
			}
			//System.out.println("loadCRL stop  " + (new Date().toString()));
			//System.out.println("idxBufPos " + idxBufPos);
			//System.out.println("snBufPos " + snBufPos);
			//System.out.println("snBuf.len = " + snBuf.length);
			//System.out.println("idxBuf.len = " + idxBuf.length);
			//System.out.println("sort start " + (new Date().toString()));
			// qs.sort1(idxBuf, idxBufPos, snBuf);
			Comparator<Integer> comp = new Comparator<Integer>(){
				 public int compare(Integer a, Integer b){
					  int i;
					  for(i = 0 ; i < 20 ; i++ )
					  	if( snBuf[a+i] != snBuf[b+i] )
					  			break;
					  	
					  if(i == 20 )
						  return 0;
					  
					  int aa = snBuf[a+i];
					  int bb = snBuf[b+i];
					  if(aa>=0&&bb>=0){
						  if(aa>bb)
							  return 1;
						  else
							  return -1;
					  }
					  else if(aa>=0&&bb<0)
						  return -1;
					  else if(aa<0&&bb>=0)
						  return 1;
					  else{
						  if(aa>bb)
							  return 1;
						  else
							  return -1;			  
					  }
				  }
			};
			Arrays.sort(idxBuf,0,idxBufPos,comp);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("ItrusCRL.java CRL parse exception: ");
			e.printStackTrace();
		}
	}
	
}

