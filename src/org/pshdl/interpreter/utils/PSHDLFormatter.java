package org.pshdl.interpreter.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.IllegalFormatCodePointException;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatFlagsException;
import java.util.IllegalFormatPrecisionException;
import java.util.IllegalFormatWidthException;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.MissingFormatWidthException;
import java.util.TimeZone;
import java.util.UnknownFormatConversionException;
import java.util.UnknownFormatFlagsException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PSHDLFormatter {
	private Appendable a;

	private final FormatString[] formats;

	public IOException lastException;

	// %[argument_index$][flags][width][.precision][t]conversion
	private static final String formatSpecifier = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

	private static Pattern fsPattern = Pattern.compile(formatSpecifier);

	public PSHDLFormatter(String fmt) {
		this(new StringBuilder(), fmt);
	}

	public PSHDLFormatter(Appendable a, String fmt) {
		this.a = a;
		this.formats = parse(fmt);
	}

	public Appendable format(Object... args) {
		// index of last argument referenced
		int last = -1;
		// last ordinary index
		int lasto = -1;

		for (final FormatString fs : formats) {
			try {
				final int index = fs.index();
				switch (index) {
				case -2: // fixed string, "%n", or "%%"
					fs.print(null);
					break;
				case -1: // relative index
					if ((last < 0) || ((args != null) && (last > (args.length - 1))))
						throw new MissingFormatArgumentException(fs.toString());
					fs.print((args == null ? null : args[last]));
					break;
				case 0: // ordinary index
					lasto++;
					last = lasto;
					if ((args != null) && (lasto > (args.length - 1)))
						throw new MissingFormatArgumentException(fs.toString());
					fs.print((args == null ? null : args[lasto]));
					break;
				default: // explicit index
					last = index - 1;
					if ((args != null) && (last > (args.length - 1)))
						throw new MissingFormatArgumentException(fs.toString());
					fs.print((args == null ? null : args[last]));
					break;
				}
			} catch (final IOException e) {
				lastException = e;
			}
		}
		return a;
	}

	private interface FormatString {
		int index();

		void print(Object arg) throws IOException;

		@Override
		String toString();
	}

	private class FixedString implements FormatString {
		private final String s;

		FixedString(String s) {
			this.s = s;
		}

		@Override
		public int index() {
			return -2;
		}

		@Override
		public void print(Object arg) throws IOException {
			a.append(s);
		}

		@Override
		public String toString() {
			return s;
		}
	}

	private static class Conversion {
		// Byte, Short, Integer, Long, BigInteger
		// (and associated primitives due to autoboxing)
		static final char DECIMAL_INTEGER = 'd';
		static final char OCTAL_INTEGER = 'o';
		static final char HEXADECIMAL_INTEGER = 'x';
		static final char HEXADECIMAL_INTEGER_UPPER = 'X';

		// Character, Byte, Short, Integer
		// (and associated primitives due to autoboxing)
		static final char CHARACTER = 'c';
		static final char CHARACTER_UPPER = 'C';

		// java.util.Date, java.util.Calendar, long
		@SuppressWarnings("unused")
		static final char DATE_TIME = 't';
		@SuppressWarnings("unused")
		static final char DATE_TIME_UPPER = 'T';

		// if (arg.TYPE != boolean) return boolean
		// if (arg != null) return true; else return false;
		static final char BOOLEAN = 'b';
		static final char BOOLEAN_UPPER = 'B';
		// if (arg instanceof Formattable) arg.formatTo()
		// else arg.toString();
		static final char STRING = 's';
		static final char STRING_UPPER = 'S';
		// arg.hashCode()
		static final char HASHCODE = 'h';
		static final char HASHCODE_UPPER = 'H';

		static final char LINE_SEPARATOR = 'n';
		static final char PERCENT_SIGN = '%';

		static boolean isValid(char c) {
			return (isGeneral(c) || isInteger(c) || isText(c) || (c == 't') || isCharacter(c));
		}

		// Returns true iff the Conversion is applicable to all objects.
		static boolean isGeneral(char c) {
			switch (c) {
			case STRING:
			case STRING_UPPER:
			case HASHCODE:
			case HASHCODE_UPPER:
				return true;
			default:
				return false;
			}
		}

		// Returns true iff the Conversion is applicable to character.
		static boolean isCharacter(char c) {
			switch (c) {
			case CHARACTER:
			case CHARACTER_UPPER:
				return true;
			default:
				return false;
			}
		}

		// Returns true iff the Conversion is an integer type.
		static boolean isInteger(char c) {
			switch (c) {
			case BOOLEAN:
			case BOOLEAN_UPPER:
			case DECIMAL_INTEGER:
			case OCTAL_INTEGER:
			case HEXADECIMAL_INTEGER:
			case HEXADECIMAL_INTEGER_UPPER:
				return true;
			default:
				return false;
			}
		}

		// Returns true iff the Conversion does not require an argument
		static boolean isText(char c) {
			switch (c) {
			case LINE_SEPARATOR:
			case PERCENT_SIGN:
				return true;
			default:
				return false;
			}
		}
	}

	private static class DateTime {
		static final char HOUR_OF_DAY_0 = 'H'; // (00 - 23)
		static final char HOUR_0 = 'I'; // (01 - 12)
		static final char HOUR_OF_DAY = 'k'; // (0 - 23) -- like H
		static final char HOUR = 'l'; // (1 - 12) -- like I
		static final char MINUTE = 'M'; // (00 - 59)
		static final char NANOSECOND = 'N'; // (000000000 - 999999999)
		static final char MILLISECOND = 'L'; // jdk, not in gnu (000 - 999)
		static final char MILLISECOND_SINCE_EPOCH = 'Q'; // (0 - 99...?)
		static final char AM_PM = 'p'; // (am or pm)
		static final char SECONDS_SINCE_EPOCH = 's'; // (0 - 99...?)
		static final char SECOND = 'S'; // (00 - 60 - leap second)
		static final char TIME = 'T'; // (24 hour hh:mm:ss)
		static final char ZONE_NUMERIC = 'z'; // (-1200 - +1200) - ls minus?
		static final char ZONE = 'Z'; // (symbol)

		// Date
		static final char NAME_OF_DAY_ABBREV = 'a'; // 'a'
		static final char NAME_OF_DAY = 'A'; // 'A'
		static final char NAME_OF_MONTH_ABBREV = 'b'; // 'b'
		static final char NAME_OF_MONTH = 'B'; // 'B'
		static final char CENTURY = 'C'; // (00 - 99)
		static final char DAY_OF_MONTH_0 = 'd'; // (01 - 31)
		static final char DAY_OF_MONTH = 'e'; // (1 - 31) -- like d
		// * static final char ISO_WEEK_OF_YEAR_2 = 'g'; // cross %y %V
		// * static final char ISO_WEEK_OF_YEAR_4 = 'G'; // cross %Y %V
		static final char NAME_OF_MONTH_ABBREV_X = 'h'; // -- same b
		static final char DAY_OF_YEAR = 'j'; // (001 - 366)
		static final char MONTH = 'm'; // (01 - 12)
		// * static final char DAY_OF_WEEK_1 = 'u'; // (1 - 7) Monday
		// * static final char WEEK_OF_YEAR_SUNDAY = 'U'; // (0 - 53) Sunday+
		// * static final char WEEK_OF_YEAR_MONDAY_01 = 'V'; // (01 - 53)
		// Monday+
		// * static final char DAY_OF_WEEK_0 = 'w'; // (0 - 6) Sunday
		// * static final char WEEK_OF_YEAR_MONDAY = 'W'; // (00 - 53) Monday
		static final char YEAR_2 = 'y'; // (00 - 99)
		static final char YEAR_4 = 'Y'; // (0000 - 9999)

		// Composites
		static final char TIME_12_HOUR = 'r'; // (hh:mm:ss [AP]M)
		static final char TIME_24_HOUR = 'R'; // (hh:mm same as %H:%M)
		// * static final char LOCALE_TIME = 'X'; // (%H:%M:%S) - parse format?
		static final char DATE_TIME = 'c';
		// (Sat Nov 04 12:02:33 EST 1999)
		static final char DATE = 'D'; // (mm/dd/yy)
		static final char ISO_STANDARD_DATE = 'F'; // (%Y-%m-%d)
		// * static final char LOCALE_DATE = 'x'; // (mm/dd/yy)

		static boolean isValid(char c) {
			switch (c) {
			case HOUR_OF_DAY_0:
			case HOUR_0:
			case HOUR_OF_DAY:
			case HOUR:
			case MINUTE:
			case NANOSECOND:
			case MILLISECOND:
			case MILLISECOND_SINCE_EPOCH:
			case AM_PM:
			case SECONDS_SINCE_EPOCH:
			case SECOND:
			case TIME:
			case ZONE_NUMERIC:
			case ZONE:

				// Date
			case NAME_OF_DAY_ABBREV:
			case NAME_OF_DAY:
			case NAME_OF_MONTH_ABBREV:
			case NAME_OF_MONTH:
			case CENTURY:
			case DAY_OF_MONTH_0:
			case DAY_OF_MONTH:
				// * case ISO_WEEK_OF_YEAR_2:
				// * case ISO_WEEK_OF_YEAR_4:
			case NAME_OF_MONTH_ABBREV_X:
			case DAY_OF_YEAR:
			case MONTH:
				// * case DAY_OF_WEEK_1:
				// * case WEEK_OF_YEAR_SUNDAY:
				// * case WEEK_OF_YEAR_MONDAY_01:
				// * case DAY_OF_WEEK_0:
				// * case WEEK_OF_YEAR_MONDAY:
			case YEAR_2:
			case YEAR_4:

				// Composites
			case TIME_12_HOUR:
			case TIME_24_HOUR:
				// * case LOCALE_TIME:
			case DATE_TIME:
			case DATE:
			case ISO_STANDARD_DATE:
				// * case LOCALE_DATE:
				return true;
			default:
				return false;
			}
		}
	}

	private static class Flags {
		private int flags;

		static final Flags NONE = new Flags(0); // ''

		// duplicate declarations from Formattable.java
		static final Flags LEFT_JUSTIFY = new Flags(1 << 0); // '-'
		static final Flags UPPERCASE = new Flags(1 << 1); // '^'
		static final Flags ALTERNATE = new Flags(1 << 2); // '#'

		// numerics
		static final Flags PLUS = new Flags(1 << 3); // '+'
		static final Flags LEADING_SPACE = new Flags(1 << 4); // ' '
		static final Flags ZERO_PAD = new Flags(1 << 5); // '0'
		static final Flags GROUP = new Flags(1 << 6); // ','
		static final Flags PARENTHESES = new Flags(1 << 7); // '('

		// indexing
		static final Flags PREVIOUS = new Flags(1 << 8); // '<'

		private Flags(int f) {
			flags = f;
		}

		public int valueOf() {
			return flags;
		}

		public boolean contains(Flags f) {
			return (flags & f.valueOf()) == f.valueOf();
		}

		public Flags dup() {
			return new Flags(flags);
		}

		private Flags add(Flags f) {
			flags |= f.valueOf();
			return this;
		}

		public Flags remove(Flags f) {
			flags &= ~f.valueOf();
			return this;
		}

		public static Flags parse(String s) {
			final char[] ca = s.toCharArray();
			final Flags f = new Flags(0);
			for (final char element : ca) {
				final Flags v = parse(element);
				if (f.contains(v))
					throw new DuplicateFormatFlagsException(v.toString());
				f.add(v);
			}
			return f;
		}

		// parse those flags which may be provided by users
		private static Flags parse(char c) {
			switch (c) {
			case '-':
				return LEFT_JUSTIFY;
			case '#':
				return ALTERNATE;
			case '+':
				return PLUS;
			case ' ':
				return LEADING_SPACE;
			case '0':
				return ZERO_PAD;
			case ',':
				return GROUP;
			case '(':
				return PARENTHESES;
			case '<':
				return PREVIOUS;
			default:
				throw new UnknownFormatFlagsException(String.valueOf(c));
			}
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			if (contains(LEFT_JUSTIFY)) {
				sb.append('-');
			}
			if (contains(UPPERCASE)) {
				sb.append('^');
			}
			if (contains(ALTERNATE)) {
				sb.append('#');
			}
			if (contains(PLUS)) {
				sb.append('+');
			}
			if (contains(LEADING_SPACE)) {
				sb.append(' ');
			}
			if (contains(ZERO_PAD)) {
				sb.append('0');
			}
			if (contains(GROUP)) {
				sb.append(',');
			}
			if (contains(PARENTHESES)) {
				sb.append('(');
			}
			if (contains(PREVIOUS)) {
				sb.append('<');
			}
			return sb.toString();
		}
	}

	private class FormatSpecifier implements FormatString {
		private int index = -1;
		private Flags f = Flags.NONE;
		private int width;
		private int precision;
		private boolean dt = false;
		private char c;

		private int index(String s) {
			if (s != null) {
				try {
					index = Integer.parseInt(s.substring(0, s.length() - 1));
				} catch (final NumberFormatException x) {
					assert (false);
				}
			} else {
				index = 0;
			}
			return index;
		}

		@Override
		public int index() {
			return index;
		}

		private Flags flags(String s) {
			f = Flags.parse(s);
			if (f.contains(Flags.PREVIOUS)) {
				index = -1;
			}
			return f;
		}

		private int width(String s) {
			width = -1;
			if (s != null) {
				try {
					width = Integer.parseInt(s);
					if (width < 0)
						throw new IllegalFormatWidthException(width);
				} catch (final NumberFormatException x) {
					assert (false);
				}
			}
			return width;
		}

		private int precision(String s) {
			precision = -1;
			if (s != null) {
				try {
					// remove the '.'
					precision = Integer.parseInt(s.substring(1));
					if (precision < 0)
						throw new IllegalFormatPrecisionException(precision);
				} catch (final NumberFormatException x) {
					assert (false);
				}
			}
			return precision;
		}

		private char conversion(String s) {
			c = s.charAt(0);
			if (!dt) {
				if (!Conversion.isValid(c))
					throw new UnknownFormatConversionException(String.valueOf(c));
				if (Character.isUpperCase(c)) {
					f.add(Flags.UPPERCASE);
				}
				c = Character.toLowerCase(c);
				if (Conversion.isText(c)) {
					index = -2;
				}
			}
			return c;
		}

		FormatSpecifier(Matcher m) {
			int idx = 1;

			index(m.group(idx++));
			flags(m.group(idx++));
			width(m.group(idx++));
			precision(m.group(idx++));

			final String tT = m.group(idx++);
			if (tT != null) {
				dt = true;
				if (tT.equals("T")) {
					f.add(Flags.UPPERCASE);
				}
			}

			conversion(m.group(idx));

			if (dt) {
				checkDateTime();
			} else if (Conversion.isGeneral(c)) {
				checkGeneral();
			} else if (Conversion.isCharacter(c)) {
				checkCharacter();
			} else if (Conversion.isInteger(c)) {
				checkInteger();
			} else if (Conversion.isText(c)) {
				checkText();
			} else
				throw new UnknownFormatConversionException(String.valueOf(c));
		}

		@Override
		public void print(Object arg) throws IOException {
			if (dt) {
				printDateTime(arg);
				return;
			}
			switch (c) {
			case Conversion.DECIMAL_INTEGER:
			case Conversion.OCTAL_INTEGER:
			case Conversion.HEXADECIMAL_INTEGER:
			case Conversion.BOOLEAN:
			case Conversion.BOOLEAN_UPPER:
				printInteger(arg);
				break;
			case Conversion.CHARACTER:
			case Conversion.CHARACTER_UPPER:
				printCharacter(arg);
				break;
			case Conversion.STRING:
				printString(arg);
				break;
			case Conversion.HASHCODE:
				printHashCode(arg);
				break;
			case Conversion.LINE_SEPARATOR:
				a.append(System.lineSeparator());
				break;
			case Conversion.PERCENT_SIGN:
				a.append('%');
				break;
			default:
				assert false;
			}
		}

		private void printInteger(Object arg) throws IOException {
			if (arg == null) {
				print("null");
			} else if (arg instanceof Boolean) {
				print(((Boolean) arg).booleanValue() ? 1 : 0);
			} else if (arg instanceof Byte) {
				print(((Byte) arg).byteValue());
			} else if (arg instanceof Short) {
				print(((Short) arg).shortValue());
			} else if (arg instanceof Integer) {
				print(((Integer) arg).intValue());
			} else if (arg instanceof Long) {
				print(((Long) arg).longValue());
			} else if (arg instanceof BigInteger) {
				print(((BigInteger) arg));
			} else {
				failConversion(c, arg);
			}
		}

		private void printDateTime(Object arg) throws IOException {
			if (arg == null) {
				print("null");
				return;
			}
			Calendar cal = null;
			final Locale l = Locale.ROOT;

			// Instead of Calendar.setLenient(true), perhaps we should
			// wrap the IllegalArgumentException that might be thrown?
			if (arg instanceof Long) {
				// Note that the following method uses an instance of the
				// default time zone (TimeZone.getDefaultRef().
				cal = Calendar.getInstance(l);
				cal.setTimeInMillis((Long) arg);
			} else if (arg instanceof Date) {
				// Note that the following method uses an instance of the
				// default time zone (TimeZone.getDefaultRef().
				cal = Calendar.getInstance(l);
				cal.setTime((Date) arg);
			} else if (arg instanceof Calendar) {
				cal = (Calendar) ((Calendar) arg).clone();
				cal.setLenient(true);
			} else {
				failConversion(c, arg);
			}
			// Use the provided locale so that invocations of
			// localizedMagnitude() use optimizations for null.
			print(cal, c);
		}

		private void printCharacter(Object arg) throws IOException {
			if (arg == null) {
				print("null");
				return;
			}
			String s = null;
			if (arg instanceof Character) {
				s = ((Character) arg).toString();
			} else if (arg instanceof Byte) {
				final byte i = ((Byte) arg).byteValue();
				if (Character.isValidCodePoint(i)) {
					s = new String(Character.toChars(i));
				} else
					throw new IllegalFormatCodePointException(i);
			} else if (arg instanceof Short) {
				final short i = ((Short) arg).shortValue();
				if (Character.isValidCodePoint(i)) {
					s = new String(Character.toChars(i));
				} else
					throw new IllegalFormatCodePointException(i);
			} else if (arg instanceof Integer) {
				final int i = ((Integer) arg).intValue();
				if (Character.isValidCodePoint(i)) {
					s = new String(Character.toChars(i));
				} else
					throw new IllegalFormatCodePointException(i);
			} else {
				failConversion(c, arg);
			}
			print(s);
		}

		private void printString(Object arg) throws IOException {
			if (f.contains(Flags.ALTERNATE)) {
				failMismatch(Flags.ALTERNATE, 's');
			}
			if (arg == null) {
				print("null");
			} else {
				print(arg.toString());
			}
		}

		private void printHashCode(Object arg) throws IOException {
			final String s = (arg == null ? "null" : Integer.toHexString(arg.hashCode()));
			print(s);
		}

		private void print(String s) throws IOException {
			String res = s;
			if ((precision != -1) && (precision < s.length())) {
				res = s.substring(0, precision);
			}
			if (f.contains(Flags.UPPERCASE)) {
				res = s.toUpperCase();
			}
			a.append(justify(res));
		}

		private String justify(String s) {
			if (width == -1)
				return s;
			final StringBuilder sb = new StringBuilder();
			final boolean pad = f.contains(Flags.LEFT_JUSTIFY);
			final int sp = width - s.length();
			if (!pad) {
				for (int i = 0; i < sp; i++) {
					sb.append(' ');
				}
			}
			sb.append(s);
			if (pad) {
				for (int i = 0; i < sp; i++) {
					sb.append(' ');
				}
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder('%');
			// Flags.UPPERCASE is set internally for legal conversions.
			final Flags dupf = f.dup().remove(Flags.UPPERCASE);
			sb.append(dupf.toString());
			if (index > 0) {
				sb.append(index).append('$');
			}
			if (width != -1) {
				sb.append(width);
			}
			if (precision != -1) {
				sb.append('.').append(precision);
			}
			if (dt) {
				sb.append(f.contains(Flags.UPPERCASE) ? 'T' : 't');
			}
			sb.append(f.contains(Flags.UPPERCASE) ? Character.toUpperCase(c) : c);
			return sb.toString();
		}

		private void checkGeneral() {
			if (((c == Conversion.BOOLEAN) || (c == Conversion.HASHCODE)) && f.contains(Flags.ALTERNATE)) {
				failMismatch(Flags.ALTERNATE, c);
			}
			// '-' requires a width
			if ((width == -1) && f.contains(Flags.LEFT_JUSTIFY))
				throw new MissingFormatWidthException(toString());
			checkBadFlags(Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
		}

		private void checkDateTime() {
			if (precision != -1)
				throw new IllegalFormatPrecisionException(precision);
			if (!DateTime.isValid(c))
				throw new UnknownFormatConversionException("t" + c);
			checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
			// '-' requires a width
			if ((width == -1) && f.contains(Flags.LEFT_JUSTIFY))
				throw new MissingFormatWidthException(toString());
		}

		private void checkCharacter() {
			if (precision != -1)
				throw new IllegalFormatPrecisionException(precision);
			checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
			// '-' requires a width
			if ((width == -1) && f.contains(Flags.LEFT_JUSTIFY))
				throw new MissingFormatWidthException(toString());
		}

		private void checkInteger() {
			checkNumeric();
			if (precision != -1)
				throw new IllegalFormatPrecisionException(precision);

			if (c == Conversion.DECIMAL_INTEGER) {
				checkBadFlags(Flags.ALTERNATE);
			} else if (c == Conversion.OCTAL_INTEGER) {
				checkBadFlags(Flags.GROUP);
			} else {
				checkBadFlags(Flags.GROUP);
			}
		}

		private void checkBadFlags(Flags... badFlags) {
			for (final Flags badFlag : badFlags)
				if (f.contains(badFlag)) {
					failMismatch(badFlag, c);
				}
		}

		private void checkNumeric() {
			if ((width != -1) && (width < 0))
				throw new IllegalFormatWidthException(width);

			if ((precision != -1) && (precision < 0))
				throw new IllegalFormatPrecisionException(precision);

			// '-' and '0' require a width
			if ((width == -1) && (f.contains(Flags.LEFT_JUSTIFY) || f.contains(Flags.ZERO_PAD)))
				throw new MissingFormatWidthException(toString());

			// bad combination
			if ((f.contains(Flags.PLUS) && f.contains(Flags.LEADING_SPACE)) || (f.contains(Flags.LEFT_JUSTIFY) && f.contains(Flags.ZERO_PAD)))
				throw new IllegalFormatFlagsException(f.toString());
		}

		private void checkText() {
			if (precision != -1)
				throw new IllegalFormatPrecisionException(precision);
			switch (c) {
			case Conversion.PERCENT_SIGN:
				if ((f.valueOf() != Flags.LEFT_JUSTIFY.valueOf()) && (f.valueOf() != Flags.NONE.valueOf()))
					throw new IllegalFormatFlagsException(f.toString());
				// '-' requires a width
				if ((width == -1) && f.contains(Flags.LEFT_JUSTIFY))
					throw new MissingFormatWidthException(toString());
				break;
			case Conversion.LINE_SEPARATOR:
				if (width != -1)
					throw new IllegalFormatWidthException(width);
				if (f.valueOf() != Flags.NONE.valueOf())
					throw new IllegalFormatFlagsException(f.toString());
				break;
			default:
				assert false;
			}
		}

		private void print(byte value) throws IOException {
			long v = value;
			if ((value < 0) && ((c == Conversion.OCTAL_INTEGER) || (c == Conversion.HEXADECIMAL_INTEGER))) {
				v += (1L << 8);
				assert v >= 0 : v;
			}
			print(v);
		}

		private void print(short value) throws IOException {
			long v = value;
			if ((value < 0) && ((c == Conversion.OCTAL_INTEGER) || (c == Conversion.HEXADECIMAL_INTEGER))) {
				v += (1L << 16);
				assert v >= 0 : v;
			}
			print(v);
		}

		private void print(int value) throws IOException {
			long v = value;
			if ((value < 0) && ((c == Conversion.OCTAL_INTEGER) || (c == Conversion.HEXADECIMAL_INTEGER))) {
				v += (1L << 32);
				assert v >= 0 : v;
			}
			print(v);
		}

		private void print(long value) throws IOException {

			final StringBuilder sb = new StringBuilder();

			if (c == Conversion.DECIMAL_INTEGER) {
				final boolean neg = value < 0;
				char[] va;
				if (value < 0) {
					va = Long.toString(value, 10).substring(1).toCharArray();
				} else {
					va = Long.toString(value, 10).toCharArray();
				}

				// leading sign indicator
				leadingSign(sb, neg);

				// the value
				localizedMagnitude(sb, va, f, adjustWidth(width, f, neg));

				// trailing sign indicator
				trailingSign(sb, neg);
			} else if (c == Conversion.OCTAL_INTEGER) {
				checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
				final String s = Long.toOctalString(value);
				final int len = (f.contains(Flags.ALTERNATE) ? s.length() + 1 : s.length());

				// apply ALTERNATE (radix indicator for octal) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE)) {
					sb.append('0');
				}
				if (f.contains(Flags.ZERO_PAD)) {
					for (int i = 0; i < (width - len); i++) {
						sb.append('0');
					}
				}
				sb.append(s);
			} else if (c == Conversion.HEXADECIMAL_INTEGER) {
				checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
				String s = Long.toHexString(value);
				final int len = (f.contains(Flags.ALTERNATE) ? s.length() + 2 : s.length());

				// apply ALTERNATE (radix indicator for hex) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE)) {
					sb.append(f.contains(Flags.UPPERCASE) ? "0X" : "0x");
				}
				if (f.contains(Flags.ZERO_PAD)) {
					for (int i = 0; i < (width - len); i++) {
						sb.append('0');
					}
				}
				if (f.contains(Flags.UPPERCASE)) {
					s = s.toUpperCase();
				}
				sb.append(s);
			} else if ((c == Conversion.BOOLEAN) || (c == Conversion.BOOLEAN_UPPER)) {
				checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
				String s = Long.toBinaryString(value);
				final int len = (f.contains(Flags.ALTERNATE) ? s.length() + 2 : s.length());

				// apply ALTERNATE (radix indicator for hex) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE)) {
					sb.append(f.contains(Flags.UPPERCASE) ? "0B" : "0b");
				}
				if (f.contains(Flags.ZERO_PAD)) {
					for (int i = 0; i < (width - len); i++) {
						sb.append('0');
					}
				}
				if (f.contains(Flags.UPPERCASE)) {
					s = s.toUpperCase();
				}
				sb.append(s);
			}

			// justify based on width
			a.append(justify(sb.toString()));
		}

		// neg := val < 0
		private StringBuilder leadingSign(StringBuilder sb, boolean neg) {
			if (!neg) {
				if (f.contains(Flags.PLUS)) {
					sb.append('+');
				} else if (f.contains(Flags.LEADING_SPACE)) {
					sb.append(' ');
				}
			} else {
				if (f.contains(Flags.PARENTHESES)) {
					sb.append('(');
				} else {
					sb.append('-');
				}
			}
			return sb;
		}

		// neg := val < 0
		private StringBuilder trailingSign(StringBuilder sb, boolean neg) {
			if (neg && f.contains(Flags.PARENTHESES)) {
				sb.append(')');
			}
			return sb;
		}

		private void print(BigInteger value) throws IOException {
			final StringBuilder sb = new StringBuilder();
			final boolean neg = value.signum() == -1;
			final BigInteger v = value.abs();

			// leading sign indicator
			leadingSign(sb, neg);

			// the value
			if (c == Conversion.DECIMAL_INTEGER) {
				final char[] va = v.toString().toCharArray();
				localizedMagnitude(sb, va, f, adjustWidth(width, f, neg));
			} else if (c == Conversion.OCTAL_INTEGER) {
				final String s = v.toString(8);

				int len = s.length() + sb.length();
				if (neg && f.contains(Flags.PARENTHESES)) {
					len++;
				}

				// apply ALTERNATE (radix indicator for octal) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE)) {
					len++;
					sb.append('0');
				}
				if (f.contains(Flags.ZERO_PAD)) {
					for (int i = 0; i < (width - len); i++) {
						sb.append('0');
					}
				}
				sb.append(s);
			} else if (c == Conversion.HEXADECIMAL_INTEGER) {
				String s = v.toString(16);

				int len = s.length() + sb.length();
				if (neg && f.contains(Flags.PARENTHESES)) {
					len++;
				}

				// apply ALTERNATE (radix indicator for hex) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE)) {
					len += 2;
					sb.append(f.contains(Flags.UPPERCASE) ? "0X" : "0x");
				}
				if (f.contains(Flags.ZERO_PAD)) {
					for (int i = 0; i < (width - len); i++) {
						sb.append('0');
					}
				}
				if (f.contains(Flags.UPPERCASE)) {
					s = s.toUpperCase();
				}
				sb.append(s);
			}

			// trailing sign indicator
			trailingSign(sb, (value.signum() == -1));

			// justify based on width
			a.append(justify(sb.toString()));
		}

		private int adjustWidth(int width, Flags f, boolean neg) {
			int newW = width;
			if ((newW != -1) && neg && f.contains(Flags.PARENTHESES)) {
				newW--;
			}
			return newW;
		}

		private void print(Calendar t, char c) throws IOException {
			final StringBuilder sb = new StringBuilder();
			print(sb, t, c);

			// justify based on width
			String s = justify(sb.toString());
			if (f.contains(Flags.UPPERCASE)) {
				s = s.toUpperCase();
			}

			a.append(s);
		}

		private Appendable print(StringBuilder sb, Calendar t, char c) throws IOException {
			assert (width == -1);
			switch (c) {
			case DateTime.HOUR_OF_DAY_0: // 'H' (00 - 23)
			case DateTime.HOUR_0: // 'I' (01 - 12)
			case DateTime.HOUR_OF_DAY: // 'k' (0 - 23) -- like H
			case DateTime.HOUR: { // 'l' (1 - 12) -- like I
				int i = t.get(Calendar.HOUR_OF_DAY);
				if ((c == DateTime.HOUR_0) || (c == DateTime.HOUR)) {
					i = ((i == 0) || (i == 12) ? 12 : i % 12);
				}
				final Flags flags = ((c == DateTime.HOUR_OF_DAY_0) || (c == DateTime.HOUR_0) ? Flags.ZERO_PAD : Flags.NONE);
				sb.append(localizedMagnitude(null, i, flags, 2));
				break;
			}
			case DateTime.MINUTE: { // 'M' (00 - 59)
				final int i = t.get(Calendar.MINUTE);
				final Flags flags = Flags.ZERO_PAD;
				sb.append(localizedMagnitude(null, i, flags, 2));
				break;
			}
			case DateTime.NANOSECOND: { // 'N' (000000000 - 999999999)
				final int i = t.get(Calendar.MILLISECOND) * 1000000;
				final Flags flags = Flags.ZERO_PAD;
				sb.append(localizedMagnitude(null, i, flags, 9));
				break;
			}
			case DateTime.MILLISECOND: { // 'L' (000 - 999)
				final int i = t.get(Calendar.MILLISECOND);
				final Flags flags = Flags.ZERO_PAD;
				sb.append(localizedMagnitude(null, i, flags, 3));
				break;
			}
			case DateTime.MILLISECOND_SINCE_EPOCH: { // 'Q' (0 - 99...?)
				final long i = t.getTimeInMillis();
				final Flags flags = Flags.NONE;
				sb.append(localizedMagnitude(null, i, flags, width));
				break;
			}
			case DateTime.AM_PM: { // 'p' (am or pm)
				// Calendar.AM = 0, Calendar.PM = 1, LocaleElements defines
				// upper
				final String[] ampm = { "AM", "PM" };
				final String s = ampm[t.get(Calendar.AM_PM)];
				sb.append(s.toLowerCase(Locale.ROOT));
				break;
			}
			case DateTime.SECONDS_SINCE_EPOCH: { // 's' (0 - 99...?)
				final long i = t.getTimeInMillis() / 1000;
				final Flags flags = Flags.NONE;
				sb.append(localizedMagnitude(null, i, flags, width));
				break;
			}
			case DateTime.SECOND: { // 'S' (00 - 60 - leap second)
				final int i = t.get(Calendar.SECOND);
				final Flags flags = Flags.ZERO_PAD;
				sb.append(localizedMagnitude(null, i, flags, 2));
				break;
			}
			case DateTime.ZONE_NUMERIC: { // 'z' ({-|+}####) - ls minus?
				int i = t.get(Calendar.ZONE_OFFSET) + t.get(Calendar.DST_OFFSET);
				final boolean neg = i < 0;
				sb.append(neg ? '-' : '+');
				if (neg) {
					i = -i;
				}
				final int min = i / 60000;
				// combine minute and hour into a single integer
				final int offset = ((min / 60) * 100) + (min % 60);
				final Flags flags = Flags.ZERO_PAD;

				sb.append(localizedMagnitude(null, offset, flags, 4));
				break;
			}
			case DateTime.ZONE: { // 'Z' (symbol)
				final TimeZone tz = t.getTimeZone();
				sb.append(tz.getDisplayName((t.get(Calendar.DST_OFFSET) != 0), TimeZone.SHORT, Locale.ROOT));
				break;
			}

				// Date
			case DateTime.NAME_OF_DAY_ABBREV: // 'a'
			case DateTime.NAME_OF_DAY: { // 'A'
				final int i = t.get(Calendar.DAY_OF_WEEK);
				final Locale lt = Locale.ROOT;
				final DateFormatSymbols dfs = DateFormatSymbols.getInstance(lt);
				if (c == DateTime.NAME_OF_DAY) {
					sb.append(dfs.getWeekdays()[i]);
				} else {
					sb.append(dfs.getShortWeekdays()[i]);
				}
				break;
			}
			case DateTime.NAME_OF_MONTH_ABBREV: // 'b'
			case DateTime.NAME_OF_MONTH_ABBREV_X: // 'h' -- same b
			case DateTime.NAME_OF_MONTH: { // 'B'
				final int i = t.get(Calendar.MONTH);
				final Locale lt = Locale.ROOT;
				final DateFormatSymbols dfs = DateFormatSymbols.getInstance(lt);
				if (c == DateTime.NAME_OF_MONTH) {
					sb.append(dfs.getMonths()[i]);
				} else {
					sb.append(dfs.getShortMonths()[i]);
				}
				break;
			}
			case DateTime.CENTURY: // 'C' (00 - 99)
			case DateTime.YEAR_2: // 'y' (00 - 99)
			case DateTime.YEAR_4: { // 'Y' (0000 - 9999)
				int i = t.get(Calendar.YEAR);
				int size = 2;
				switch (c) {
				case DateTime.CENTURY:
					i /= 100;
					break;
				case DateTime.YEAR_2:
					i %= 100;
					break;
				case DateTime.YEAR_4:
					size = 4;
					break;
				}
				final Flags flags = Flags.ZERO_PAD;
				sb.append(localizedMagnitude(null, i, flags, size));
				break;
			}
			case DateTime.DAY_OF_MONTH_0: // 'd' (01 - 31)
			case DateTime.DAY_OF_MONTH: { // 'e' (1 - 31) -- like d
				final int i = t.get(Calendar.DATE);
				final Flags flags = (c == DateTime.DAY_OF_MONTH_0 ? Flags.ZERO_PAD : Flags.NONE);
				sb.append(localizedMagnitude(null, i, flags, 2));
				break;
			}
			case DateTime.DAY_OF_YEAR: { // 'j' (001 - 366)
				final int i = t.get(Calendar.DAY_OF_YEAR);
				final Flags flags = Flags.ZERO_PAD;
				sb.append(localizedMagnitude(null, i, flags, 3));
				break;
			}
			case DateTime.MONTH: { // 'm' (01 - 12)
				final int i = t.get(Calendar.MONTH) + 1;
				final Flags flags = Flags.ZERO_PAD;
				sb.append(localizedMagnitude(null, i, flags, 2));
				break;
			}

				// Composites
			case DateTime.TIME: // 'T' (24 hour hh:mm:ss - %tH:%tM:%tS)
			case DateTime.TIME_24_HOUR: { // 'R' (hh:mm same as %H:%M)
				final char sep = ':';
				print(sb, t, DateTime.HOUR_OF_DAY_0).append(sep);
				print(sb, t, DateTime.MINUTE);
				if (c == DateTime.TIME) {
					sb.append(sep);
					print(sb, t, DateTime.SECOND);
				}
				break;
			}
			case DateTime.TIME_12_HOUR: { // 'r' (hh:mm:ss [AP]M)
				final char sep = ':';
				print(sb, t, DateTime.HOUR_0).append(sep);
				print(sb, t, DateTime.MINUTE).append(sep);
				print(sb, t, DateTime.SECOND).append(' ');
				// this may be in wrong place for some locales
				final StringBuilder tsb = new StringBuilder();
				print(tsb, t, DateTime.AM_PM);
				sb.append(tsb.toString().toUpperCase(Locale.ROOT));
				break;
			}
			case DateTime.DATE_TIME: { // 'c' (Sat Nov 04 12:02:33 EST 1999)
				final char sep = ' ';
				print(sb, t, DateTime.NAME_OF_DAY_ABBREV).append(sep);
				print(sb, t, DateTime.NAME_OF_MONTH_ABBREV).append(sep);
				print(sb, t, DateTime.DAY_OF_MONTH_0).append(sep);
				print(sb, t, DateTime.TIME).append(sep);
				print(sb, t, DateTime.ZONE).append(sep);
				print(sb, t, DateTime.YEAR_4);
				break;
			}
			case DateTime.DATE: { // 'D' (mm/dd/yy)
				final char sep = '/';
				print(sb, t, DateTime.MONTH).append(sep);
				print(sb, t, DateTime.DAY_OF_MONTH_0).append(sep);
				print(sb, t, DateTime.YEAR_2);
				break;
			}
			case DateTime.ISO_STANDARD_DATE: { // 'F' (%Y-%m-%d)
				final char sep = '-';
				print(sb, t, DateTime.YEAR_4).append(sep);
				print(sb, t, DateTime.MONTH).append(sep);
				print(sb, t, DateTime.DAY_OF_MONTH_0);
				break;
			}
			default:
				assert false;
			}
			return sb;
		}

		// -- Methods to support throwing exceptions --

		private void failMismatch(Flags f, char c) {
			final String fs = f.toString();
			throw new FormatFlagsConversionMismatchException(fs, c);
		}

		private void failConversion(char c, Object arg) {
			throw new IllegalFormatConversionException(c, arg.getClass());
		}

		private StringBuilder localizedMagnitude(StringBuilder sb, long value, Flags f, int width) {
			final char[] va = Long.toString(value, 10).toCharArray();
			return localizedMagnitude(sb, va, f, width);
		}

		private StringBuilder localizedMagnitude(StringBuilder sb, char[] value, Flags f, int width) {
			final int begin = sb.length();

			final char zero = '0';

			// determine localized grouping separator and size
			char grpSep = '\0';
			int grpSize = -1;
			char decSep = '\0';

			int len = value.length;
			int dot = len;
			for (int j = 0; j < len; j++) {
				if (value[j] == '.') {
					dot = j;
					break;
				}
			}

			if (dot < len) {
				decSep = '.';
			}

			if (f.contains(Flags.GROUP)) {
				grpSep = ',';
				grpSize = 3;
			}

			// localize the digits inserting group separators as necessary
			for (int j = 0; j < len; j++) {
				if (j == dot) {
					sb.append(decSep);
					// no more group separators after the decimal separator
					grpSep = '\0';
					continue;
				}

				final char c = value[j];
				sb.append((char) ((c - '0') + zero));
				if ((grpSep != '\0') && (j != (dot - 1)) && (((dot - j) % grpSize) == 1)) {
					sb.append(grpSep);
				}
			}

			// apply zero padding
			len = sb.length();
			if ((width != -1) && f.contains(Flags.ZERO_PAD)) {
				for (int k = 0; k < (width - len); k++) {
					sb.insert(begin, zero);
				}
			}

			return sb;
		}
	}

	private static void checkText(String s, int start, int end) {
		for (int i = start; i < end; i++) {
			// Any '%' found in the region starts an invalid format specifier.
			if (s.charAt(i) == '%') {
				final char c = (i == (end - 1)) ? '%' : s.charAt(i + 1);
				throw new UnknownFormatConversionException(String.valueOf(c));
			}
		}
	}

	/**
	 * Finds format specifiers in the format string.
	 */
	private FormatString[] parse(String s) {
		final ArrayList<FormatString> al = new ArrayList<>();
		final Matcher m = fsPattern.matcher(s);
		for (int i = 0, len = s.length(); i < len;) {
			if (m.find(i)) {
				// Anything between the start of the string and the beginning
				// of the format specifier is either fixed text or contains
				// an invalid format string.
				if (m.start() != i) {
					// Make sure we didn't miss any invalid format specifiers
					checkText(s, i, m.start());
					// Assume previous characters were fixed text
					al.add(new FixedString(s.substring(i, m.start())));
				}

				al.add(new FormatSpecifier(m));
				i = m.end();
			} else {
				// No more valid format specifiers. Check for possible invalid
				// format specifiers.
				checkText(s, i, len);
				// The rest of the string is fixed text
				al.add(new FixedString(s.substring(i)));
				break;
			}
		}
		return al.toArray(new FormatString[al.size()]);
	}

	public void clear() {
		a = new StringBuilder();
	}

}
