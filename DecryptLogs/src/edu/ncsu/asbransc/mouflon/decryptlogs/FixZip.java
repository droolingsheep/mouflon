/*Mouflon: an Android app for collecting and reporting application usage.
    Copyright (C) 2013 Andrew Branscomb

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package edu.ncsu.asbransc.mouflon.decryptlogs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
//Code in this file from http://stackoverflow.com/questions/11039079/cannot-extract-file-from-zip-archive-created-on-android-device-os-specific
public class FixZip {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//System.out.println(args[0]);
		try {
			fixInvalidZipFile(new File(args[0]));
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	/**
	* Replace wrong local file header byte
	* http://sourceforge.net/tracker/?func=detail&aid=3477810&group_id=14481&atid=114481
	* Applies to Android API 9-13
	* @param zip file
	* @throws IOException
	*/
	public static void fixInvalidZipFile(File zip) throws IOException 
	{
	    RandomAccessFile r = new RandomAccessFile(zip, "rw");
	    try
	    {
	        long eocd_offset = findEOCDRecord(r);

	        if (eocd_offset > 0)
	        {
	            r.seek(eocd_offset + 16);  // offset of first CDE in EOCD               
	            long cde_offset = readInt(r);  // read offset of first Central Directory Entry
	            long lfh_offset = 0;
	            long fskip, dskip;

	            while (true)
	            {
	                r.seek(cde_offset);
	                if (readInt(r) != CDE_SIGNATURE)  // got off sync!
	                    return;

	                r.seek(cde_offset + 20);  // compressed file size offset                
	                fskip = readInt(r);

	                // fix the header
	                //
	                r.seek(lfh_offset + 7);
	                short localFlagsHi = r.readByte();  // hi-order byte of local header flags (general purpose)
	                r.seek(cde_offset + 9);
	                short realFlagsHi = r.readByte();  // hi-order byte of central directory flags (general purpose)
	                if (localFlagsHi != realFlagsHi)
	                { // in latest versions this bug is fixed, so we're checking is bug exists.
	                    r.seek(lfh_offset + 7);
	                    r.write(realFlagsHi);
	                }

	                //  calculate offset of next Central Directory Entry
	                //
	                r.seek(cde_offset + 28);  // offset of variable CDE parts length in CDE
	                dskip = 46;  // length of fixed CDE part
	                dskip += readShort(r);  // file name
	                dskip += readShort(r);  // extra field
	                dskip += readShort(r);  // file comment

	                cde_offset += dskip;
	                if (cde_offset >= eocd_offset)  // finished!
	                    break;              

	                // calculate offset of next Local File Header
	                //
	                r.seek(lfh_offset + 26);  // offset of variable LFH parts length in LFH
	                fskip += readShort(r);  // file name
	                fskip += readShort(r);  // extra field
	                fskip += 30;  // length of fixed LFH part
	                fskip += 16;  // length of Data Descriptor (written after file data)

	                lfh_offset += fskip;
	            }
	        }
	    }
	    finally
	    {
	        r.close();
	    }
	}

	//http://www.pkware.com/documents/casestudies/APPNOTE.TXT
	private static final int LFH_SIGNATURE = 0x04034b50;
	private static final int DD_SIGNATURE = 0x08074b50;
	private static final int CDE_SIGNATURE = 0x02014b50;
	private static final int EOCD_SIGNATURE = 0x06054b50;

	/** Find an offset of End Of Central Directory record in file */
	private static long findEOCDRecord(RandomAccessFile f) throws IOException
	{
	    long result = f.length() - 22; // 22 is minimal EOCD record length
	    while (result > 0)
	    {
	        f.seek(result);

	        if (readInt(f) == EOCD_SIGNATURE) return result;

	        result--;
	    }
	    return -1;
	}

	/** Read a 4-byte integer from file converting endianness. */
	private static int readInt(RandomAccessFile f) throws IOException
	{
	    int result = 0;
	    result |= f.read();
	    result |= (f.read() << 8);
	    result |= (f.read() << 16);
	    result |= (f.read() << 24);
	    return result;
	}

	/** Read a 2-byte integer from file converting endianness. */
	private static short readShort(RandomAccessFile f) throws IOException
	{
	    short result = 0;
	    result |= f.read();
	    result |= (f.read() << 8);
	    return result;
	}
}
