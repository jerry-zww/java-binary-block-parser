/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.jbbp.it;

import com.igormaznitsa.jbbp.*;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.mapper.JBBPMapperCustomFieldProcessor;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import org.junit.Test;
import com.igormaznitsa.jbbp.io.*;
import java.io.*;

/**
 * Test to parse RLE encoded snapshots in well-known Z80 format (v.1) for
 * ZX-Spectrum emulators.
 */
public class Z80_v1_ParsingTest extends AbstractParserIntegrationTest {

  @Bin(type = BinType.BIT)
  class EmulFlags {

    byte interruptmode;
    byte issue2emulation;
    byte doubleintfreq;
    byte videosync;
    byte inputdevice;
  }

  @Bin(type = BinType.BIT)
  class Flags {

    byte reg_r_bit7;
    byte bordercolor;
    byte basic_samrom;
    byte compressed;
    byte nomeaning;
  }

  @Bin
  class Z80Snapshot {

    byte reg_a;
    byte reg_f;
    short reg_bc;
    short reg_hl;
    short reg_pc;
    short reg_sp;
    byte reg_ir;
    byte reg_r;

    Flags flags;

    short reg_de;
    short reg_bc_alt;
    short reg_de_alt;
    short reg_hl_alt;
    byte reg_a_alt;
    byte reg_f_alt;
    short reg_iy;
    short reg_ix;
    byte iff;
    byte iff2;

    EmulFlags emulFlags;

    @Bin(custom = true)
    byte[] data;
  }

  private static final JBBPParser z80Parser = JBBPParser.prepare(
          "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "byte [_] data;"
  );

  private static class RLEDataEncoder implements JBBPOutVarProcessor {

    public boolean processVarOut(final JBBPOut context, final JBBPBitOutputStream outStream, final Object... args) throws IOException {
      final byte[] unpackedData = (byte[]) args[1];
      if (((Number) args[0]).intValue() == 0) {
        context.Byte(unpackedData);
      }
      else {

        int value = -1;
        int counter = 0;

        for (int i = 0; i < unpackedData.length; i++) {
          final int cur = unpackedData[i] & 0xFF;
          if (value < 0) {
            value = cur;
            counter = 1;
          }
          else {
            if (value == cur) {
              counter++;
              if (counter == 0xFF) {
                context.Byte(0xED, 0xED, counter, cur);
                value = -1;
                counter = 0;
              }
            }
            else if (counter >= 5 || (value == 0xED && counter > 1)) {
              context.Byte(0xED, 0xED, counter, value);
              counter = 1;
              value = cur;
            }
            else {
              while (counter != 0) {
                context.Byte(value);
                counter--;
              }
              if (value == 0xED) {
                context.Byte(cur);
                value = -1;
                counter = 0;
              }
              else {
                counter = 1;
                value = cur;
              }
            }
          }
        }

        if (counter < 5) {
          while (counter != 0) {
            context.Byte(value);
            counter--;
          }
        }
        else {
          context.Byte(0xED, 0xED, counter, value);
        }

        context.Byte(0x00, 0xED, 0xED, 0x00);
      }
      return true;
    }
  }

  @Test
  public void testRLEEncoding() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xED, (byte) 0xED, 1, 2, 3}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 0, new byte[]{(byte) 0xED, (byte) 0xED, 1, 2, 3}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xED, (byte) 0xED, 2, (byte) 0xED, 1, 2, 3, 0x00, (byte) 0xED, (byte) 0xED, 0x00}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 1, new byte[]{(byte) 0xED, (byte) 0xED, 1, 2, 3}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xED, 0x00, (byte) 0xED, (byte) 0xED, 0x05, 0x00, 0x00, (byte) 0xED, (byte) 0xED, 0x00}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 1, new byte[]{(byte) 0xED, 0, 0, 0, 0, 0, 0}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xED, (byte) 0xED, 8, 5, 1, 2, 3, 0x00, (byte) 0xED, (byte) 0xED, 0x00}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 1, new byte[]{5, 5, 5, 5, 5, 5, 5, 5, 1, 2, 3}).End().toByteArray());
  }

  private static class DataProcessor implements JBBPMapperCustomFieldProcessor {

    public Object prepareObjectForMapping(JBBPFieldStruct parsedBlock, Bin annotation, Field field) {
      if (field.getName().equals("data")) {
        final byte[] data = parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();

        if (parsedBlock.findFieldForPathAndType("flags.compressed", JBBPFieldBit.class).getAsBool()) {
          // RLE compressed
          final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length << 1);
          int i = 0;

          // check end marker
          assertEquals(0x00, data[data.length - 1] & 0xFF);
          assertEquals(0xED, data[data.length - 2] & 0xFF);
          assertEquals(0xED, data[data.length - 3] & 0xFF);
          assertEquals(0x00, data[data.length - 4] & 0xFF);

          final int len = data.length - 4;

          while (i < len) {
            final int a = data[i++] & 0xFF;
            if (a == 0xED) {
              final int b = data[i++] & 0xFF;
              if (b == 0xED) {
                int num = data[i++] & 0xFF;
                final int val = data[i++] & 0xFF;
                while (num > 0) {
                  baos.write(val);
                  num--;
                }
              }
              else {
                baos.write(a);
                baos.write(b);
              }
            }
            else {
              baos.write(a);
            }
          }
          return baos.toByteArray();
        }
        else {
          // uncompressed
          return data;
        }
      }
      else {
        fail("Unexpected field");
        return null;
      }
    }
  }

  private Z80Snapshot assertParseAndPackBack(final String name) throws Exception {
    final Z80Snapshot z80sn;

    final InputStream resource = getResourceAsInputStream(name);
    try {
      z80sn = z80Parser.parse(resource).mapTo(Z80Snapshot.class, new DataProcessor());
    }
    finally {
      JBBPUtils.closeQuietly(resource);
    }

    // form the same from parsed
    final byte[] packed = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN, JBBPBitOrder.LSB0).
            Byte(z80sn.reg_a, z80sn.reg_f).
            Short(z80sn.reg_bc, z80sn.reg_hl, z80sn.reg_pc, z80sn.reg_sp).
            Byte(z80sn.reg_ir, z80sn.reg_r).
            Bit(z80sn.flags.reg_r_bit7).
            Bits(JBBPBitNumber.BITS_3, z80sn.flags.bordercolor).
            Bit(z80sn.flags.basic_samrom, z80sn.flags.compressed).
            Bits(JBBPBitNumber.BITS_2, z80sn.flags.nomeaning).
            Short(z80sn.reg_de, z80sn.reg_bc_alt, z80sn.reg_de_alt, z80sn.reg_hl_alt).
            Byte(z80sn.reg_a_alt, z80sn.reg_f_alt).
            Short(z80sn.reg_iy, z80sn.reg_ix).
            Byte(z80sn.iff, z80sn.iff2).
            Bits(JBBPBitNumber.BITS_2, z80sn.emulFlags.interruptmode).
            Bit(z80sn.emulFlags.issue2emulation, z80sn.emulFlags.doubleintfreq).
            Bits(JBBPBitNumber.BITS_2, z80sn.emulFlags.videosync, z80sn.emulFlags.inputdevice).
            Var(new RLEDataEncoder(), z80sn.flags.compressed, z80sn.data).
            End().toByteArray();

    assertResource(name, packed);
    return z80sn;
  }

  @Test
  public void testParseAndWriteTestZ80WithoutCheckOfFields() throws Exception {
    assertParseAndPackBack("test1.z80");
    assertParseAndPackBack("test2.z80");
    assertParseAndPackBack("test3.z80");
    assertParseAndPackBack("test4.z80");
  }  
  
  @Test
  public void testParseAndWriteTestZ80WithCheckOfFields() throws Exception {
    final Z80Snapshot z80sn = assertParseAndPackBack("test.z80");

    assertEquals(0x7E, z80sn.reg_a & 0xFF);
    assertEquals(0x86, z80sn.reg_f & 0xFF);
    assertEquals(0x7A74, z80sn.reg_bc & 0xFFFF);
    assertEquals(0x7430, z80sn.reg_hl & 0xFFFF);

    assertEquals(12198, z80sn.reg_pc & 0xFFFF);
    assertEquals(65330, z80sn.reg_sp & 0xFFFF);

    assertEquals(0x3F, z80sn.reg_ir & 0xFF);
    assertEquals(0x1A, z80sn.reg_r & 0xFF);

    assertEquals(0, z80sn.flags.reg_r_bit7);
    assertEquals(2, z80sn.flags.bordercolor);
    assertEquals(0, z80sn.flags.basic_samrom);
    assertEquals(1, z80sn.flags.compressed);
    assertEquals(0, z80sn.flags.nomeaning);

    assertEquals(0x742B, z80sn.reg_de & 0xFFFF);
    assertEquals(0x67C6, z80sn.reg_bc_alt & 0xFFFF);
    assertEquals(0x3014, z80sn.reg_de_alt & 0xFFFF);
    assertEquals(0x3461, z80sn.reg_hl_alt & 0xFFFF);
    assertEquals(0x00, z80sn.reg_a_alt & 0xFF);
    assertEquals(0x46, z80sn.reg_f_alt & 0xFF);
    assertEquals(0x5C3A, z80sn.reg_iy & 0xFFFF);
    assertEquals(0x03D4, z80sn.reg_ix & 0xFFFF);
    assertEquals(0xFF, z80sn.iff & 0xFF);
    assertEquals(0xFF, z80sn.iff2 & 0xFF);

    assertEquals(1, z80sn.emulFlags.interruptmode);
    assertEquals(0, z80sn.emulFlags.issue2emulation);
    assertEquals(0, z80sn.emulFlags.doubleintfreq);
    assertEquals(0, z80sn.emulFlags.videosync);
    assertEquals(0, z80sn.emulFlags.inputdevice);

    assertEquals(49152, z80sn.data.length);
    int summ = 0;
    for (final byte b : z80sn.data) {
      summ += b & 0xFF;
    }
    assertTrue(summ > 0);
  }

}
