package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.InetAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.common.IoUtils;
import com.j256.simplejmx.common.JmxResourceInfo;

public class PublishAllBeanWrapperTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = PublishAllBeanWrapperTest.class.getSimpleName();
	private static final int FOO_VALUE = 1459243;
	private static final int BAR_VALUE = 1423459243;
	private static final int BAZ_VALUE = 63456352;

	private static JmxServer server;
	private static InetAddress serverAddress;

	@BeforeClass
	public static void beforeClass() throws Exception {
		serverAddress = InetAddress.getByName("127.0.0.1");
		server = new JmxServer(serverAddress, DEFAULT_PORT);
		server.start();
	}

	@AfterClass
	public static void afterClass() {
		if (server != null) {
			server.stop();
			server = null;
			System.gc();
		}
	}

	@Test
	public void testRegister() throws Exception {
		TestObject obj = new TestObject();
		JmxResourceInfo resourceInfo = new JmxResourceInfo(DOMAIN_NAME, OBJECT_NAME, "description");
		PublishAllBeanWrapper publishAll = new PublishAllBeanWrapper(obj, resourceInfo);
		JmxClient client = null;
		try {
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			server.register(publishAll);

			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
			assertEquals(BAR_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "bar"));

			int val = FOO_VALUE + 1;
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", val);
			assertEquals(val, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo");
			assertEquals(0, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
			val = FOO_VALUE + 2;
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", val);
			assertEquals(val, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "unknown");
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}
			try {
				client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "unknown", FOO_VALUE);
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}
			try {
				client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "unknown");
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}
			try {
				client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "getFoo");
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}

		} finally {
			server.unregister(publishAll);
			IoUtils.closeQuietly(client);
		}
	}

	@Test
	public void testSubClass() throws Exception {
		SubClassTestObject obj = new SubClassTestObject();
		JmxResourceInfo resourceInfo = new JmxResourceInfo(DOMAIN_NAME, null, "description");
		PublishAllBeanWrapper publishAll = new PublishAllBeanWrapper(obj, resourceInfo);
		JmxClient client = null;
		try {
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			server.register(publishAll);

			obj.bar = 37634345;
			obj.baz = 678934522;

			assertEquals(obj.bar, client.getAttribute(DOMAIN_NAME, obj.getClass().getSimpleName(), "bar"));
			assertEquals(obj.baz, client.getAttribute(DOMAIN_NAME, obj.getClass().getSimpleName(), "baz"));
			assertEquals(4, client.getAttributesInfo(DOMAIN_NAME, obj.getClass().getSimpleName()).length);
		} finally {
			server.unregister(resourceInfo);
			IoUtils.closeQuietly(client);
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCoverage() {
		PublishAllBeanWrapper wrapper = new PublishAllBeanWrapper();
		wrapper.setDelegate(this);
		wrapper.setTarget(this);
		wrapper.setJmxResourceInfo(new JmxResourceInfo(DOMAIN_NAME, OBJECT_NAME, "description"));
	}

	/* ======================================================================= */

	protected static class TestObject {
		private int foo = FOO_VALUE;
		public int bar = BAR_VALUE;
		public final int baz = BAZ_VALUE;

		public int getFoo() {
			return foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}

		public void resetFoo() {
			this.foo = 0;
		}

		public void resetFoo(int newValue) {
			this.foo = newValue;
		}

		public boolean isSomething() {
			return true;
		}

		public boolean is() {
			return false;
		}

		public boolean get() {
			return false;
		}

		public void set(int foo) {
			// nothing here
		}
	}

	protected static class SubClassTestObject extends TestObject {
		private int baz;

		public int getBaz() {
			return baz;
		}

		public void setBaz(int baz) {
			this.baz = baz;
		}
	}
}
