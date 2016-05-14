/*
 * Part of CANFlasherUTNL - Flash tool for NXP LPC11C22/24 devices.
 * http://www.fischl.de/can/bootloader/canflasherutnl/
 *
 * Copyright (C) 2016  Thomas Fischl 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.*;

/**
 * Convert intel hex file to binary data field
 * 
 * @author Thomas Fischl
 */
public class HexParser {

    public static final int RECORD_TYPE_DATA = 0x00;
    public static final int RECORD_TYPE_EOF = 0x01;
    public static final int RECORD_TYPE_EXT_SEGMENT_ADDRESS = 0x02;
    public static final int RECORD_TYPE_SEGMENT_START_ADDRESS = 0x03;
    public static final int RECORD_TYPE_EXT_LINEAR_ADDRESS = 0x04;
    public static final int RADIX = 16;

    /**
     * Read HEX file and parse it
     * 
     * @param r Reader for hex file
     * @param dm Target device memory
     * @return Last address
     * @throws IOException Error while parsing file
     */
    static int read(Reader r, DeviceMemory dm) throws IOException {

        BufferedReader br = new BufferedReader(r);

        String record;
        int lineNum = 0;
        int extendedAddress = 0;
        int segmentAddress = 0;
        int endAddress = 0;

        while ((record = br.readLine()) != null) {

            lineNum++;

            int dataLength = Integer.parseInt(record.substring(1, 3), RADIX);
            int address = Integer.parseInt(record.substring(3, 7), RADIX);
            int recordType = Integer.parseInt(record.substring(7, 9), RADIX);

            // checksum
            byte sum = 0;
            for (int i = 0; i < dataLength + 5; i++) {
                sum += (byte) (Integer.parseInt(record.substring(i * 2 + 1, (i * 2 + 3)), RADIX));
            }
            if (sum != 0) {
                throw new IllegalArgumentException("invalid checksum in line " + lineNum);
            }

            switch (recordType) {
                case RECORD_TYPE_DATA:

                    for (int i = 0; i < dataLength; i++) {
                        byte value = (byte) (Integer.parseInt(record.substring(i * 2 + 9, (i * 2 + 11)), RADIX));

                        int a = address + extendedAddress + segmentAddress;

                        dm.writeMemoryData(a, value);
                        
                        address++;
                    }

                    break;
                case RECORD_TYPE_EOF:
                    break;
                case RECORD_TYPE_EXT_LINEAR_ADDRESS:
                    extendedAddress = Integer.parseInt(record.substring(9, 13), 16) << 16;
                    break;
                case RECORD_TYPE_EXT_SEGMENT_ADDRESS:
                    segmentAddress = Integer.parseInt(record.substring(9, 13), 16) << 4;
                    break;
                case RECORD_TYPE_SEGMENT_START_ADDRESS:
                    // ignore start address
                    break;
                default:
                    System.err.println("Unknown record type in line " + lineNum + ": " + String.format("%02x", recordType));
                    break;
            }
        }

        return endAddress + 1;
    }

}
