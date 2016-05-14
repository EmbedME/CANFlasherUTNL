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

import de.fischl.usbtin.*;
import java.util.Arrays;

/**
 * Provides CANOpen functionality for Service Data Object (SDO)
 * 
 * @author Thomas Fischl
 */
public class USBtinSDO extends USBtin implements CANMessageListener {

    /** Contains last received message */
    CANMessage receivedMsg = null;
    
    /** Syncronization object for received message */
    private final Object syncObj = new Object();
    
    /**
     * Construct new USBtin with SDO support
     */
    public USBtinSDO() {
        super();
        addMessageListener(this);
    }
    
    /**
     * This method is called every time a CAN message is received.
     * 
     * @param canmsg Received CAN message
     */
    @Override
    public void receiveCANMessage(CANMessage canmsg) {

        synchronized(syncObj) {
            receivedMsg = canmsg;
            syncObj.notify();
        }        
    }
    
    /**
     * Transmit given CAN messsage, wait for response and check the answer.
     * 
     * @param msg CAN message to send
     * @param expected Expected answer
     * @return Response
     * @throws USBtinException Error while transmitting message
     */
    public CANMessage transmit(CANMessage msg, int expected)  throws USBtinException {
        receivedMsg = null;
        this.send(msg);
        
        long timeout = 1000;
        
        try {
            synchronized(syncObj) {
                
                long starttime = System.currentTimeMillis();
                while ((System.currentTimeMillis() - starttime) < timeout) {
                    
                    if ((receivedMsg != null) && (receivedMsg.getData().length >= 1)) {
                        if (receivedMsg.getData()[0] == expected)
                            return receivedMsg;
                        else 
                            throw new USBtinException("sdo_transmit: not expected answer (is: " + receivedMsg.getData()[0] + ", expected: " + expected + ")");
                    }
                    
                    syncObj.wait(timeout);
                }                                    
                throw new USBtinException("sdo_transmit: timeout");
            }
        } catch (InterruptedException ie) {
            throw new USBtinException("sdo_transmit: interrupted exception ");
        }
    }
    
    /**
     * Read object at given index
     * 
     * @param obj_idx Object index
     * @param sub_idx Subindex
     * @return Content of object
     * @throws USBtinException Error while reading object
     */
    public byte[] read(int obj_idx, int sub_idx) throws USBtinException {
        
        CANMessage msg = transmit(
                new CANMessage(0x67d, new byte[]{0x40, (byte)(obj_idx & 0xff), (byte)((obj_idx >> 8) & 0xff), (byte)sub_idx, 0x00, 0x00, 0x00, 0x00}),
                0x43);
        
        return Arrays.copyOfRange(msg.getData(), 4, 8);
    }
    
    /**
     * Write to object in expedited mode
     * 
     * @param obj_idx Object index
     * @param sub_idx Subindex
     * @param data Byte field to send
     * @throws USBtinException Error while writing data
     */
    public void writeExpedited(int obj_idx, int sub_idx, byte[] data)  throws USBtinException {
        
        byte[] msgdata = new byte[8];
            
        switch (data.length) {
            case 4: msgdata[0] = 0x23; break;
            case 2: msgdata[0] = 0x2b; break;
            case 1: msgdata[0] = 0x2f; break;
            default: msgdata[0] = 0x22; break;
        }

        msgdata[1] = (byte)(obj_idx & 0xff);
        msgdata[2] = (byte)((obj_idx >> 8) & 0xff);
        msgdata[3] = (byte)sub_idx;
        for (int i = 0; i < data.length; i++) {
            msgdata[4 + i] = data[i];
        }

        transmit(new CANMessage(0x67d, msgdata), 0x60);        
    }
    
    /**
     * Write data to object in segmented mode
     * 
     * @param obj_idx Object index
     * @param sub_idx Subindex
     * @param data Byte field to write
     * @throws USBtinException Error while writing data
     */
    public void writeSegmented(int obj_idx, int sub_idx, byte[] data)  throws USBtinException{        

        int bytesleft = data.length;        
        CANMessage msg = new CANMessage(0x67d, new byte[]{0x21, (byte)(obj_idx & 0xff), (byte)((obj_idx >> 8) & 0xff), (byte)sub_idx, (byte)(bytesleft & 0xff), (byte)((bytesleft >> 8) & 0xff), 0x00, 0x00});
        transmit(msg, 0x60);                        
            
        boolean toggle = false;
        int pos = 0;
        while (bytesleft > 0) {
            receivedMsg = null;

            int sendbyte = bytesleft;
            if (sendbyte > 7) sendbyte = 7;

            byte[] msgdata = new byte[8];

            if (bytesleft > 7) msgdata[0] = 0;
            else if (bytesleft == 7) msgdata[0] = 0x01;
            else if (bytesleft == 6) msgdata[0] = 0x03;
            else if (bytesleft == 5) msgdata[0] = 0x05;
            else if (bytesleft == 4) msgdata[0] = 0x07;
            else if (bytesleft == 3) msgdata[0] = 0x09;
            else if (bytesleft == 2) msgdata[0] = 0x0B;
            else if (bytesleft == 1) msgdata[0] = 0x0D;

            if (toggle) msgdata[0] |= 0x10;                

            for (int i = 0; i < sendbyte; i++) {
                msgdata[1 + i] = data[pos];
                pos ++;
            }

            bytesleft -= sendbyte;                

            int expected;
            if (!toggle) expected = 0x20;
            else expected = 0x30;
            
            msg = new CANMessage(0x67d, msgdata);
            transmit(msg, expected);

            toggle = !toggle;                        
        }
    }
}
