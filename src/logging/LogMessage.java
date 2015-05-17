/**
 * Copyright (c) 2013 Aaron Faanes
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package logging;

/**
 * A simple message.
 * 
 * @author Aaron Faanes
 * @param <T>
 *            the type of message
 * 
 */
public class LogMessage<T> {

	private long timestamp;
	private Object sender;
	private String category;
	private T message;

	public LogMessage(Object sender, String category, T message) {
		this(System.currentTimeMillis(), sender, category, message);
	}

	public LogMessage(String category, T message) {
		this(System.currentTimeMillis(), null, category, message);
	}

	public LogMessage(T message) {
		this(System.currentTimeMillis(), null, null, message);
	}

	public LogMessage(long timestamp, Object sender, String category, T message) {
		this.timestamp = timestamp;
		this.sender = sender;
		this.category = category;
		this.message = message;
	}

	public T getMessage() {
		return this.message;
	}

	public <U> LogMessage<U> changeMessage(U newMessage) {
		return new LogMessage<U>(timestamp, sender, category, newMessage);
	}

	public LogMessage<T> changeSender(Object sender) {
		return new LogMessage<T>(timestamp, sender, category, message);
	}

	public <U> LogMessage<U> changeSender(Object sender, U message) {
		return new LogMessage<U>(timestamp, sender, category, message);
	}

	/**
	 * The logical category of this message. It is optional and may be null.
	 * 
	 * @return the category of this message
	 */
	public String getCategory() {
		return this.category;
	}

	/**
	 * The logical source of this message. It may be null.
	 * 
	 * @return the sender of this message
	 */
	public Object getSender() {
		return this.sender;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public String toString() {
		String message = "";

		if (getSender() != null && getSender().toString().length() > 0) {
			message += getSender().toString() + ": ";
		}

		if (getMessage() != null) {
			message += getMessage().toString();
		} else if (getCategory() != null) {
			message += getCategory().toString();
		}
		return message;
	}
}
