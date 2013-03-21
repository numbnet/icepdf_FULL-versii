/*
 * Copyright 2006-2012 ICEsoft Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.jpedal.jbig2.segment.symboldictionary;

import org.jpedal.jbig2.JBIG2Exception;
import org.jpedal.jbig2.decoders.ArithmeticDecoderStats;
import org.jpedal.jbig2.decoders.DecodeIntResult;
import org.jpedal.jbig2.decoders.HuffmanDecoder;
import org.jpedal.jbig2.decoders.JBIG2StreamDecoder;
import org.jpedal.jbig2.image.JBIG2Bitmap;
import org.jpedal.jbig2.segment.Segment;
import org.jpedal.jbig2.segment.tables.JBIG2CodeTable;
import org.jpedal.jbig2.util.BinaryOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SymbolDictionarySegment extends Segment {

    private int noOfExportedSymbols;
    private int noOfNewSymbols;

    short[] symbolDictionaryAdaptiveTemplateX = new short[4], symbolDictionaryAdaptiveTemplateY = new short[4];
    short[] symbolDictionaryRAdaptiveTemplateX = new short[2], symbolDictionaryRAdaptiveTemplateY = new short[2];

    private JBIG2Bitmap[] bitmaps;

    private SymbolDictionaryFlags symbolDictionaryFlags = new SymbolDictionaryFlags();

    private ArithmeticDecoderStats genericRegionStats;
    private ArithmeticDecoderStats refinementRegionStats;

    public SymbolDictionarySegment(JBIG2StreamDecoder streamDecoder) {
        super(streamDecoder);
    }

    public void readSegment() throws IOException, JBIG2Exception {

        if (JBIG2StreamDecoder.debug)
            System.out.println("==== Read Segment Symbol Dictionary ====");

        /** read symbol dictionary flags */
        readSymbolDictionaryFlags();

        List codeTables = new ArrayList();
        int numberOfInputSymbols = 0;
        int noOfReferredToSegments = segmentHeader.getReferredToSegmentCount();
        int[] referredToSegments = segmentHeader.getReferredToSegments();

        for (int i = 0; i < noOfReferredToSegments; i++) {
            Segment seg = decoder.findSegment(referredToSegments[i]);
            int type = seg.getSegmentHeader().getSegmentType();

            if (type == Segment.SYMBOL_DICTIONARY) {
                numberOfInputSymbols += ((SymbolDictionarySegment) seg).noOfExportedSymbols;
            } else if (type == Segment.TABLES) {
                codeTables.add(seg);
            }
        }

        int symbolCodeLength = 0;
        int i = 1;
        while (i < numberOfInputSymbols + noOfNewSymbols) {
            symbolCodeLength++;
            i <<= 1;
        }

        JBIG2Bitmap[] bitmaps = new JBIG2Bitmap[numberOfInputSymbols + noOfNewSymbols];

        int k = 0;
        SymbolDictionarySegment inputSymbolDictionary = null;
        for (i = 0; i < noOfReferredToSegments; i++) {
            Segment seg = decoder.findSegment(referredToSegments[i]);
            if (seg.getSegmentHeader().getSegmentType() == Segment.SYMBOL_DICTIONARY) {
                inputSymbolDictionary = (SymbolDictionarySegment) seg;
                for (int j = 0; j < inputSymbolDictionary.noOfExportedSymbols; j++) {
                    bitmaps[k++] = inputSymbolDictionary.bitmaps[j];
                }
            }
        }

        int[][] huffmanDHTable = null;
        int[][] huffmanDWTable = null;

        int[][] huffmanBMSizeTable = null;
        int[][] huffmanAggInstTable = null;

        boolean sdHuffman = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF) != 0;
        int sdHuffmanDifferenceHeight = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_DH);
        int sdHuffmanDiferrenceWidth = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_DW);
        int sdHuffBitmapSize = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_BM_SIZE);
        int sdHuffAggregationInstances = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_AGG_INST);

        i = 0;
        if (sdHuffman) {
            if (sdHuffmanDifferenceHeight == 0) {
                huffmanDHTable = HuffmanDecoder.huffmanTableD;
            } else if (sdHuffmanDifferenceHeight == 1) {
                huffmanDHTable = HuffmanDecoder.huffmanTableE;
            } else {
                huffmanDHTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
            }

            if (sdHuffmanDiferrenceWidth == 0) {
                huffmanDWTable = HuffmanDecoder.huffmanTableB;
            } else if (sdHuffmanDiferrenceWidth == 1) {
                huffmanDWTable = HuffmanDecoder.huffmanTableC;
            } else {
                huffmanDWTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
            }

            if (sdHuffBitmapSize == 0) {
                huffmanBMSizeTable = HuffmanDecoder.huffmanTableA;
            } else {
                huffmanBMSizeTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
            }

            if (sdHuffAggregationInstances == 0) {
                huffmanAggInstTable = HuffmanDecoder.huffmanTableA;
            } else {
                huffmanAggInstTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
            }
        }

        int contextUsed = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.BITMAP_CC_USED);
        int sdTemplate = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_TEMPLATE);

        if (!sdHuffman) {
            if (contextUsed != 0 && inputSymbolDictionary != null) {
                arithmeticDecoder.resetGenericStats(sdTemplate, inputSymbolDictionary.genericRegionStats);
            } else {
                arithmeticDecoder.resetGenericStats(sdTemplate, null);
            }
            arithmeticDecoder.resetIntStats(symbolCodeLength);
            arithmeticDecoder.start();
        }

        int sdRefinementAggregate = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_REF_AGG);
        int sdRefinementTemplate = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_R_TEMPLATE);
        if (sdRefinementAggregate != 0) {
            if (contextUsed != 0 && inputSymbolDictionary != null) {
                arithmeticDecoder.resetRefinementStats(sdRefinementTemplate, inputSymbolDictionary.refinementRegionStats);
            } else {
                arithmeticDecoder.resetRefinementStats(sdRefinementTemplate, null);
            }
        }

        int deltaWidths[] = new int[noOfNewSymbols];

        int deltaHeight = 0;
        i = 0;

        while (i < noOfNewSymbols) {

            int instanceDeltaHeight = 0;

            if (sdHuffman) {
                instanceDeltaHeight = huffmanDecoder.decodeInt(huffmanDHTable).intResult();
            } else {
                instanceDeltaHeight = arithmeticDecoder.decodeInt(arithmeticDecoder.iadhStats).intResult();
            }

            if (instanceDeltaHeight < 0 && -instanceDeltaHeight >= deltaHeight) {
                if (JBIG2StreamDecoder.debug)
                    System.out.println("Bad delta-height value in JBIG2 symbol dictionary");
            }

            deltaHeight += instanceDeltaHeight;
            int symbolWidth = 0;
            int totalWidth = 0;
            int j = i;

            while (true) {

                int deltaWidth = 0;

                DecodeIntResult decodeIntResult;
                if (sdHuffman) {
                    decodeIntResult = huffmanDecoder.decodeInt(huffmanDWTable);
                } else {
                    decodeIntResult = arithmeticDecoder.decodeInt(arithmeticDecoder.iadwStats);
                }

                if (!decodeIntResult.booleanResult())
                    break;

                deltaWidth = decodeIntResult.intResult();

                if (deltaWidth < 0 && -deltaWidth >= symbolWidth) {
                    if (JBIG2StreamDecoder.debug)
                        System.out.println("Bad delta-width value in JBIG2 symbol dictionary");
                }

                symbolWidth += deltaWidth;

                if (sdHuffman && sdRefinementAggregate == 0) {
                    deltaWidths[i] = symbolWidth;
                    totalWidth += symbolWidth;

                } else if (sdRefinementAggregate == 1) {

                    int refAggNum = 0;

                    if (sdHuffman) {
                        refAggNum = huffmanDecoder.decodeInt(huffmanAggInstTable).intResult();
                    } else {
                        refAggNum = arithmeticDecoder.decodeInt(arithmeticDecoder.iaaiStats).intResult();
                    }

                    if (refAggNum == 1) {

                        int symbolID = 0, referenceDX = 0, referenceDY = 0;

                        if (sdHuffman) {
                            symbolID = decoder.readBits(symbolCodeLength);
                            referenceDX = huffmanDecoder.decodeInt(HuffmanDecoder.huffmanTableO).intResult();
                            referenceDY = huffmanDecoder.decodeInt(HuffmanDecoder.huffmanTableO).intResult();

                            decoder.consumeRemainingBits();
                            arithmeticDecoder.start();
                        } else {
                            symbolID = (int) arithmeticDecoder.decodeIAID(symbolCodeLength, arithmeticDecoder.iaidStats);
                            referenceDX = arithmeticDecoder.decodeInt(arithmeticDecoder.iardxStats).intResult();
                            referenceDY = arithmeticDecoder.decodeInt(arithmeticDecoder.iardyStats).intResult();
                        }

                        JBIG2Bitmap referredToBitmap = bitmaps[symbolID];

                        JBIG2Bitmap bitmap = new JBIG2Bitmap(symbolWidth, deltaHeight, arithmeticDecoder, huffmanDecoder, mmrDecoder);
                        bitmap.readGenericRefinementRegion(sdRefinementTemplate, false, referredToBitmap, referenceDX, referenceDY, symbolDictionaryRAdaptiveTemplateX,
                                symbolDictionaryRAdaptiveTemplateY);

                        bitmaps[numberOfInputSymbols + i] = bitmap;

                    } else {
                        JBIG2Bitmap bitmap = new JBIG2Bitmap(symbolWidth, deltaHeight, arithmeticDecoder, huffmanDecoder, mmrDecoder);
                        bitmap.readTextRegion(sdHuffman, true, refAggNum, 0, numberOfInputSymbols + i, null, symbolCodeLength, bitmaps, 0, 0, false, 1, 0,
                                HuffmanDecoder.huffmanTableF, HuffmanDecoder.huffmanTableH, HuffmanDecoder.huffmanTableK, HuffmanDecoder.huffmanTableO, HuffmanDecoder.huffmanTableO,
                                HuffmanDecoder.huffmanTableO, HuffmanDecoder.huffmanTableO, HuffmanDecoder.huffmanTableA, sdRefinementTemplate, symbolDictionaryRAdaptiveTemplateX,
                                symbolDictionaryRAdaptiveTemplateY, decoder);

                        bitmaps[numberOfInputSymbols + i] = bitmap;
                    }
                } else {
                    JBIG2Bitmap bitmap = new JBIG2Bitmap(symbolWidth, deltaHeight, arithmeticDecoder, huffmanDecoder, mmrDecoder);
                    bitmap.readBitmap(false, sdTemplate, false, false, null, symbolDictionaryAdaptiveTemplateX, symbolDictionaryAdaptiveTemplateY, 0);
                    bitmaps[numberOfInputSymbols + i] = bitmap;
                }

                i++;
            }

            if (sdHuffman && sdRefinementAggregate == 0) {
                int bmSize = huffmanDecoder.decodeInt(huffmanBMSizeTable).intResult();
                decoder.consumeRemainingBits();

                JBIG2Bitmap collectiveBitmap = new JBIG2Bitmap(totalWidth, deltaHeight, arithmeticDecoder, huffmanDecoder, mmrDecoder);

                if (bmSize == 0) {

                    int padding = totalWidth % 8;
                    int bytesPerRow = (int) Math.ceil(totalWidth / 8d);

                    //short[] bitmap = new short[totalWidth];
                    //decoder.readByte(bitmap);
                    int size = deltaHeight * ((totalWidth + 7) >> 3);
                    short[] bitmap = new short[size];
                    decoder.readByte(bitmap);

                    short[][] logicalMap = new short[deltaHeight][bytesPerRow];
                    int count = 0;
                    for (int row = 0; row < deltaHeight; row++) {
                        for (int col = 0; col < bytesPerRow; col++) {
                            logicalMap[row][col] = bitmap[count];
                            count++;
                        }
                    }

                    int collectiveBitmapRow = 0, collectiveBitmapCol = 0;

                    for (int row = 0; row < deltaHeight; row++) {
                        for (int col = 0; col < bytesPerRow; col++) {
                            if (col == (bytesPerRow - 1)) { // this is the last
                                // byte in the row
                                short currentByte = logicalMap[row][col];
                                for (int bitPointer = 7; bitPointer >= padding; bitPointer--) {
                                    short mask = (short) (1 << bitPointer);
                                    int bit = (currentByte & mask) >> bitPointer;

                                    collectiveBitmap.setPixel(collectiveBitmapCol, collectiveBitmapRow, bit);
                                    collectiveBitmapCol++;
                                }
                                collectiveBitmapRow++;
                                collectiveBitmapCol = 0;
                            } else {
                                short currentByte = logicalMap[row][col];
                                for (int bitPointer = 7; bitPointer >= 0; bitPointer--) {
                                    short mask = (short) (1 << bitPointer);
                                    int bit = (currentByte & mask) >> bitPointer;

                                    collectiveBitmap.setPixel(collectiveBitmapCol, collectiveBitmapRow, bit);
                                    collectiveBitmapCol++;
                                }
                            }
                        }
                    }

                } else {
                    collectiveBitmap.readBitmap(true, 0, false, false, null, null, null, bmSize);
                }

                int x = 0;
                while (j < i) {
                    bitmaps[numberOfInputSymbols + j] = collectiveBitmap.getSlice(x, 0, deltaWidths[j], deltaHeight);
                    x += deltaWidths[j];

                    j++;
                }
            }
        }

        this.bitmaps = new JBIG2Bitmap[noOfExportedSymbols];

        int j = i = 0;
        boolean export = false;
        while (i < numberOfInputSymbols + noOfNewSymbols) {

            int run = 0;
            if (sdHuffman) {
                run = huffmanDecoder.decodeInt(HuffmanDecoder.huffmanTableA).intResult();
            } else {
                run = arithmeticDecoder.decodeInt(arithmeticDecoder.iaexStats).intResult();
            }

            if (export) {
                for (int cnt = 0; cnt < run; cnt++) {
                    this.bitmaps[j++] = bitmaps[i++];
                }
            } else {
                i += run;
            }

            export = !export;
        }

        int contextRetained = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.BITMAP_CC_RETAINED);
        if (!sdHuffman && contextRetained == 1) {
            genericRegionStats = genericRegionStats.copy();
            if (sdRefinementAggregate == 1) {
                refinementRegionStats = refinementRegionStats.copy();
            }
        }

        /** consume any remaining bits */
        decoder.consumeRemainingBits();
    }

    private void readSymbolDictionaryFlags() throws IOException {
        /** extract symbol dictionary flags */
        short[] symbolDictionaryFlagsField = new short[2];
        decoder.readByte(symbolDictionaryFlagsField);

        int flags = BinaryOperation.getInt16(symbolDictionaryFlagsField);
        symbolDictionaryFlags.setFlags(flags);

        if (JBIG2StreamDecoder.debug)
            System.out.println("symbolDictionaryFlags = " + flags);

        // symbol dictionary AT flags
        int sdHuff = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF);
        int sdTemplate = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_TEMPLATE);
        if (sdHuff == 0) {
            if (sdTemplate == 0) {
                symbolDictionaryAdaptiveTemplateX[0] = readATValue();
                symbolDictionaryAdaptiveTemplateY[0] = readATValue();
                symbolDictionaryAdaptiveTemplateX[1] = readATValue();
                symbolDictionaryAdaptiveTemplateY[1] = readATValue();
                symbolDictionaryAdaptiveTemplateX[2] = readATValue();
                symbolDictionaryAdaptiveTemplateY[2] = readATValue();
                symbolDictionaryAdaptiveTemplateX[3] = readATValue();
                symbolDictionaryAdaptiveTemplateY[3] = readATValue();
            } else {
                symbolDictionaryAdaptiveTemplateX[0] = readATValue();
                symbolDictionaryAdaptiveTemplateY[0] = readATValue();
            }
        }

        // symbol dictionary refinement AT flags
        int refAgg = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_REF_AGG);
        int sdrTemplate = symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_R_TEMPLATE);
        if (refAgg != 0 && sdrTemplate == 0) {
            symbolDictionaryRAdaptiveTemplateX[0] = readATValue();
            symbolDictionaryRAdaptiveTemplateY[0] = readATValue();
            symbolDictionaryRAdaptiveTemplateX[1] = readATValue();
            symbolDictionaryRAdaptiveTemplateY[1] = readATValue();
        }

        /** extract no of exported symbols */
        short[] noOfExportedSymbolsField = new short[4];
        decoder.readByte(noOfExportedSymbolsField);

        int noOfExportedSymbols = BinaryOperation.getInt32(noOfExportedSymbolsField);
        this.noOfExportedSymbols = noOfExportedSymbols;

        if (JBIG2StreamDecoder.debug)
            System.out.println("noOfExportedSymbols = " + noOfExportedSymbols);

        /** extract no of new symbols */
        short[] noOfNewSymbolsField = new short[4];
        decoder.readByte(noOfNewSymbolsField);

        int noOfNewSymbols = BinaryOperation.getInt32(noOfNewSymbolsField);
        this.noOfNewSymbols = noOfNewSymbols;

        if (JBIG2StreamDecoder.debug)
            System.out.println("noOfNewSymbols = " + noOfNewSymbols);
    }

    public int getNoOfExportedSymbols() {
        return noOfExportedSymbols;
    }

    public void setNoOfExportedSymbols(int noOfExportedSymbols) {
        this.noOfExportedSymbols = noOfExportedSymbols;
    }

    public int getNoOfNewSymbols() {
        return noOfNewSymbols;
    }

    public void setNoOfNewSymbols(int noOfNewSymbols) {
        this.noOfNewSymbols = noOfNewSymbols;
    }

    public JBIG2Bitmap[] getBitmaps() {
        return bitmaps;
    }

    public SymbolDictionaryFlags getSymbolDictionaryFlags() {
        return symbolDictionaryFlags;
    }

    public void setSymbolDictionaryFlags(SymbolDictionaryFlags symbolDictionaryFlags) {
        this.symbolDictionaryFlags = symbolDictionaryFlags;
    }

    private ArithmeticDecoderStats getGenericRegionStats() {
        return genericRegionStats;
    }

    private void setGenericRegionStats(ArithmeticDecoderStats genericRegionStats) {
        this.genericRegionStats = genericRegionStats;
    }

    private void setRefinementRegionStats(ArithmeticDecoderStats refinementRegionStats) {
        this.refinementRegionStats = refinementRegionStats;
    }

    private ArithmeticDecoderStats getRefinementRegionStats() {
        return refinementRegionStats;
    }
}
