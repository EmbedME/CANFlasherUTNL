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

import java.util.Arrays;

/**
 * Represents the memory structure of a target device. A device consists of
 * memory regions which can be programmed.
 *
 * @author Thomas Fischl
 */
public class DeviceMemory {

    /**
     * Represents target memory data
     */
    protected byte[] data;

    /**
     * Size of one sector
     */
    protected int sectorSize;

    /**
     * Minimum written address
     */
    protected int wroteMin;

    /**
     * Maximum written address
     */
    protected int wroteMax;

    /**
     * Get minimum written address
     *
     * @return Minimum written address
     */
    public int getWroteMin() {
        return wroteMin;
    }

    /**
     * Get maximum written address
     * 
     * @return Maximum written address
     */
    public int getWroteMax() {
        return wroteMax;
    }

    /**
     * Get mimimum written sector
     * 
     * @return  Minimum written sector
     */
    public int getWroteSectorMin() {
        return wroteMin / sectorSize;
    }

    /**
     * Get maximum written sector
     * 
     * @return Maximum written sector
     */
    public int getWroteSectorMax() {
        return wroteMax / sectorSize;
    }

    /**
     * Get start address of given sector
     * 
     * @param sector Sector identifier
     * @return Start address of this sector
     */
    public int getSectorStartAddress(int sector) {
        return sector * sectorSize;
    }

    /**
     * Construct target memory device
     * 
     * @param size Size of target memory
     * @param sectorSize Sector size
     */
    public DeviceMemory(int size, int sectorSize) {
        this.data = new byte[size];
        Arrays.fill(data, (byte) 0xff);
        this.wroteMin = size;
        this.wroteMax = -1;
        this.sectorSize = sectorSize;
    }

    /**
     * Write one byte to the target memory
     * 
     * @param address Address
     * @param value Value
     */
    public void writeMemoryData(int address, byte value) {

        this.data[address] = value;

        if (this.wroteMin > address) {
            this.wroteMin = address;
        }
        if (this.wroteMax < address) {
            this.wroteMax = address;
        }
    }

    /**
     * Get binary data of given sector
     * 
     * @param sector Sector identifier
     * @return Data field of given sector
     */
    public byte[] getSector(int sector) {
        
        int lastaddress = ((sector + 1) * sectorSize) - 1;
        
        // TODO: optimize this, to get only written data. Be aware of alignment!
        
        //if (lastaddress > wroteMax) lastaddress = wroteMax;
        
        // 4 byte alignment
        //while (lastaddress % 4 != 0) lastaddress ++;     

        return Arrays.copyOfRange(data, sector * sectorSize, lastaddress + 1);
    }

    /**
     * Calculate and set checksum
     */
    public void insertChecksum() {

        int checksum = 0;

        for (int i = 0; i < 7; i++) {
            long vector = 0;
            vector |= data[i * 4 + 0] & 0xff;
            vector |= (data[i * 4 + 1] & 0xff) << 8;
            vector |= (data[i * 4 + 2] & 0xff) << 16;
            vector |= (data[i * 4 + 3] & 0xff) << 24;
            checksum += vector;
        }

        checksum = 0 - checksum;

        for (int i = 0; i < 4; i++) {
            data[0x1C + i] = (byte) ((checksum >> (i * 8)) & 0xff);
        }
    }
}
