/*
 * Part of CANFlasherUTNL - Flash tool for NXP LPC11C22/24 devices.
 * http://www.fischl.de/can/bootloader/canflasherutnl/
 *
 * Copyright (C) 2016-2017  Thomas Fischl 
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

import de.fischl.usbtin.*;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Provides functionality to flash LPC microcontrollers via built-in CAN
 * bootloader
 * 
 * @author Thomas Fischl
 */
public class LPCFlash {

    public static final int OBJ_IDX_DEVICE_TYPE = 0x1000;
    public static final int OBJ_IDX_IDENTITY_OBJECT = 0x1018;
    public static final int OBJ_IDX_PROGRAM_DATA = 0x1F50;
    public static final int OBJ_SUB_PROGRAM_AREA = 0x01;
    public static final int OBJ_IDX_PROGRAM_CONTROL = 0x1F51;
    public static final int OBJ_SUB_PROGRAM_CONTROL = 0x01;
    public static final int OBJ_IDX_UNLOCK_CODE = 0x5000;
    public static final int OBJ_IDX_MEMORY_READ_ADDRESS = 0x5010;
    public static final int OBJ_IDX_MEMORY_READ_LENGTH = 0x5011;
    public static final int OBJ_IDX_RAM_WRITE_ADDRESS = 0x5015;
    public static final int OBJ_IDX_PREPARE_SECTORS_FOR_WRITE = 0x5020;
    public static final int OBJ_IDX_ERASE_SECTORS = 0x5030;
    public static final int OBJ_IDX_BLANK_CHECK_SECTORS = 0x5040;
    public static final int OBJ_SUB_CHECK_SECTORS = 0x01;
    public static final int OBJ_SUB_FIRST_NONBLANK_LOCATION = 0x02;
    public static final int OBJ_IDX_COPY_RAM_TO_FLASH = 0x5050;
    public static final int OBJ_SUB_FLASH_ADDRESS = 0x01;
    public static final int OBJ_SUB_RAM_ADDRESS = 0x02;
    public static final int OBJ_SUB_NUMBER_OF_BYTES = 0x03;
    public static final int OBJ_IDX_COMPARE_MEMORY = 0x5060;
    public static final int OBJ_SUB_ADDRESS1 = 0x01;
    public static final int OBJ_SUB_ADDRESS2 = 0x02;
    public static final int OBJ_SUB_OFFSET_FIRST_MISMATCH = 0x02;
    public static final int OBJ_IDX_EXECUTION_ADDRESS = 0x5070;
    public static final int OBJ_SUB_EXECUTION_ADDRESS = 0x01;
    public static final int OBJ_SUB_MODE = 0x02;
    public static final int OBJ_IDX_SERIAL_NUMBER = 0x5100;
    
    public enum GoMode {
        NO, ADDRESS, INSERTRESET
    }    
    
    /** List of listeners */
    protected ArrayList<LPCFlashListener> listeners = new ArrayList<LPCFlashListener>();
    
    /**
     * Add listener
     * 
     * @param listener Listener to add to list
     */
    public void addListener(LPCFlashListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove listener
     * 
     * @param listener Listener to remove from list
     */
    public void removeListener(LPCFlashListener listener) {
        listeners.remove(listener);
    }
        
    /**
     * Output given message to listeners
     * 
     * @param message Message to print out
     */
    protected void outputMessage(String message) {
        for (LPCFlashListener listener : listeners) {
            listener.outputMessage(message);
        }
    }
    
    /**
     * Flash fiven hex file over USBtin connected to given port
     * 
     * @param usbtinPort Port of USBtin
     * @param hexfile Filename of HEX
     * @param gomode Type of GO command after flash process
     * @param executionAddress Address to jump to
     */
    public void flash(String usbtinPort, String hexfile, GoMode gomode, int executionAddress) {

        USBtinSDO usbtinSDO = new USBtinSDO();
        
        try {
            
            DeviceMemory dm = new DeviceMemory(32 * 1024, 4 * 1024);
            
            outputMessage("Load HEX file... ");
            
            HexParser.read(new FileReader(hexfile), dm);

            outputMessage("range: " + dm.getWroteMin() + "-" + dm.getWroteMax() + " (sectors " +  dm.getWroteSectorMin() + "-" + dm.getWroteSectorMax() + ")\n");
            
            if (gomode == GoMode.INSERTRESET) {
            
                byte resetSequence[] = new byte[]{(byte)0xBF, (byte)0xF3, (byte)0x4F, (byte)0x8F, (byte)0x02, (byte)0x4A, (byte)0x03, (byte)0x4B, (byte)0xDA, (byte)0x60, (byte)0xBF, (byte)0xF3, (byte)0x4F, (byte)0x8F, (byte)0xFE, (byte)0xE7, (byte)0x04, (byte)0x00, (byte)0xFA, (byte)0x05, (byte)0x00, (byte)0xED, (byte)0x00, (byte)0xE0};
                int resetAddress = dm.getWroteMax();
                if (resetAddress < 0x200) resetAddress = 0x200;
                resetAddress = resetAddress + (4 - resetAddress % 4); // alignment
                executionAddress = resetAddress;
                
                outputMessage("Place reset function at 0x" + String.format("%X", resetAddress) + "... ");

                for (int i = 0; i < resetSequence.length; i++) {
                    dm.writeMemoryData(resetAddress + i, resetSequence[i]);
                }
                
                outputMessage("new range: " + dm.getWroteMin() + "-" + dm.getWroteMax() + " (sectors " +  dm.getWroteSectorMin() + "-" + dm.getWroteSectorMax() + ")\n");
            }

            dm.insertChecksum();

            
            outputMessage("Open USBtin... ");

            usbtinSDO.connect(usbtinPort);
            
            usbtinSDO.setFilter(new FilterChain[] {
                new FilterChain(
                    new FilterMask(0x7ff, (byte)0x00, (byte)0x00),
                    new FilterValue[] {
                        new FilterValue(0x5fd, (byte)0x00, (byte)0x00)
                    }
                )
            });
            
            outputMessage(" " + usbtinSDO.getFirmwareVersion() + "/" + usbtinSDO.getHardwareVersion() + " SN:" + usbtinSDO.getSerialNumber() + "\n");
                        
            usbtinSDO.openCANChannel(100000, USBtin.OpenMode.ACTIVE);

            // get device type
            outputMessage("Read device type... ");
            byte[] result = usbtinSDO.read(OBJ_IDX_DEVICE_TYPE, 0x00);
            outputMessage(" " + new String(result, "ISO-8859-1") + "\n");            
            
            // unlock
            outputMessage("Unlock device...\n");
            usbtinSDO.writeExpedited(OBJ_IDX_UNLOCK_CODE, 0x00, new byte[]{0x5a, 0x5a});
            
            outputMessage("Prepare erase...\n");
            // prepare sectors for erase
            usbtinSDO.writeExpedited(OBJ_IDX_PREPARE_SECTORS_FOR_WRITE, 0x00, new byte[]{(byte)dm.getWroteSectorMin(), (byte)dm.getWroteSectorMax()});
            
            outputMessage("Erase sectors...\n");            
            // erase sectors
            usbtinSDO.writeExpedited(OBJ_IDX_ERASE_SECTORS, 0x00, new byte[]{(byte)dm.getWroteSectorMin(), (byte)dm.getWroteSectorMax()});
            
            for (int s = dm.getWroteSectorMin(); s <= dm.getWroteSectorMax(); s++) {

                outputMessage("Write sector " + s + "\n");            

                // prepare payload fields
                byte [] ramStartaddress = new byte[]{0x00, 0x08, 0x00, 0x10};
                byte [] flashStartaddress = new byte[]{
                    (byte)(dm.getSectorStartAddress(s) & 0xff),
                    (byte)((dm.getSectorStartAddress(s) >> 8) & 0xff),
                    (byte)((dm.getSectorStartAddress(s) >> 16) & 0xff),
                    (byte)((dm.getSectorStartAddress(s) >> 24) & 0xff)};
                byte [] countOfBytes = new byte[]{
                    (byte)(dm.getSector(s).length & 0xff),
                    (byte)((dm.getSector(s).length >> 8) & 0xff)};
                
                
                outputMessage("  Set RAM address...\n");
                // write ram address 0x10000800
                usbtinSDO.writeExpedited(OBJ_IDX_RAM_WRITE_ADDRESS, 0x00, ramStartaddress);
            
                outputMessage("  Transfer data...\n");
                // transfer code of this sector
                usbtinSDO.writeSegmented(OBJ_IDX_PROGRAM_DATA, OBJ_SUB_PROGRAM_AREA, dm.getSector(s));            
                
                outputMessage("  Prepare write...\n");
                // prepare sector for write
                usbtinSDO.writeExpedited(OBJ_IDX_PREPARE_SECTORS_FOR_WRITE, 0x00, new byte[]{(byte)s, (byte)s});
                
                outputMessage("  Copy RAM to flash...\n");
                // copy RAM to flash
                usbtinSDO.writeExpedited(OBJ_IDX_COPY_RAM_TO_FLASH, OBJ_SUB_FLASH_ADDRESS, flashStartaddress);
                usbtinSDO.writeExpedited(OBJ_IDX_COPY_RAM_TO_FLASH, OBJ_SUB_RAM_ADDRESS, ramStartaddress);        
                usbtinSDO.writeExpedited(OBJ_IDX_COPY_RAM_TO_FLASH, OBJ_SUB_NUMBER_OF_BYTES, countOfBytes);
                
                outputMessage("  Compare...\n");
                // compare
                usbtinSDO.writeExpedited(OBJ_IDX_COMPARE_MEMORY, OBJ_SUB_ADDRESS1, ramStartaddress);
                usbtinSDO.writeExpedited(OBJ_IDX_COMPARE_MEMORY, OBJ_SUB_ADDRESS2, flashStartaddress);        
                usbtinSDO.writeExpedited(OBJ_IDX_COMPARE_MEMORY, OBJ_SUB_NUMBER_OF_BYTES, countOfBytes);
                        
            }
            
            if (gomode != GoMode.NO) {
                outputMessage("GO to 0x" + String.format("%X", executionAddress) + " ...\n");
                usbtinSDO.writeExpedited(OBJ_IDX_EXECUTION_ADDRESS, OBJ_SUB_EXECUTION_ADDRESS, new byte[]{
                        (byte)(executionAddress & 0xff),
                        (byte)((executionAddress >> 8) & 0xff),
                        (byte)((executionAddress >> 16) & 0xff),
                        (byte)((executionAddress >> 24) & 0xff)});
                usbtinSDO.writeExpedited(OBJ_IDX_PROGRAM_CONTROL, OBJ_SUB_PROGRAM_CONTROL, new byte[]{0x01}); // write program control 0x01                
            }

            
            // close the CAN channel and close the connection
            usbtinSDO.closeCANChannel();
            usbtinSDO.disconnect();
            
            outputMessage("Finished.\n");
            
        } catch (Exception ex) {
            
            try {
                usbtinSDO.disconnect();
            } catch (Exception ex1) {};
            
            // this we need because of the System.in.read()
            outputMessage("ERROR: " + ex.getMessage());
        }
    }
}
