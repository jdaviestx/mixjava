package com.jdavies.mix;

/**
 * ADD, SUB, all LOAD and STORE (including STZ), all SHIFT and all comparison operations take 
 * two units of time.  MOVE requires one unit plus two for each word moved.  MUL, NUM, CHAR
 * each require 10 units of time and DIV requires 12.  All remaining operations take 1 unit
 * of time.
 */
enum MixOpCode	{
	 NOP, 	// 0
	 ADD, 	// 1
	 SUB,   // 2
	 MUL,   // 3
	 DIV,   // 4
	 HLT,   // 5
	 SLA,   // 6
	 MOVE,   // 7
	 LDA,   // 8
	 LD1,   // 9
	 LD2,   // 10
	 LD3,   // 11
	 LD4,   // 12
	 LD5,  // 13
	 LD6,   // 14
	 LDX,   // 15
	 LDAN,   // 16
	 LD1N,   // 17
	 LD2N,   // 18
	 LD3N,   // 19
	 LD4N,   // 20
	 LD5N,   // 21
	 LD6N,   // 22
	 LDXN,   // 23
	 STA,   // 24
	 ST1,   // 25
	 ST2,  // 26
	 ST3,   // 27
	 ST4,   // 28
	 ST5,   // 29
	 ST6,   // 30
	 STX,   // 31
	 STJ,   // 32
	 STZ,   // 33
	 JBUS,   // 34
	 IOC,   // 35
	 IN,   // 36
	 OUT,   // 37
	 JRED,   // 38
	 JMP,   // 39
	 JAP,   // 40
	 J1P,  // 41
	 J2P,   // 42
	 J3P,   // 43
	 J4P,   // 44
	 J5P,   // 45
	 J6P,   // 46
	 JXP,   // 47
	 INCA,   // 48		// also ENTA if F=2
	 INC1,   // 49
	 INC2,   // 50
	 INC3,   // 51
	 INC4,   // 52
	 INC5,   // 53
	 INC6,  // 54
	 INCX,   // 55
	 CMPA,   // 56
	 CMP1,   // 57
	 CMP2,   // 58
	 CMP3,   // 59
	 CMP4,   // 60
	 CMP5,   // 61
	 CMP6,   // 62
	 CMPX  // 63
};
