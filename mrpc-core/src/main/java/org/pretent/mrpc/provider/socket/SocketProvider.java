package org.pretent.mrpc.provider.socket;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.pretent.mrpc.Handler;
import org.pretent.mrpc.Provider;
import org.pretent.mrpc.ServerConfig;
import org.pretent.mrpc.provider.Service;
import org.pretent.mrpc.register.Register;
import org.pretent.mrpc.register.RegisterFactory;
import org.pretent.mrpc.util.ClassUtils;
import org.pretent.mrpc.util.IPHelper;
import org.pretent.mrpc.util.MapHelper;
import org.pretent.mrpc.util.ResourcesFactory;

/**
 * @author pretent
 */
public class SocketProvider implements Provider {

	private static final Logger LOGGER = Logger.getLogger(SocketProvider.class);

	private ServerSocket serverSocket = null;

	private Handler handler = null;

	private String host;

	private int port;

	private Register register;

	public SocketProvider() throws Exception {
		serverSocket = new ServerSocket();
		handler = new SocketHandler();
		register = new RegisterFactory().getRegister();
		setHost(ResourcesFactory.getString(ServerConfig.KEY_STRING_HOST) == null ? "0.0.0.0"
				: ResourcesFactory.getString(ServerConfig.KEY_STRING_HOST));
		setPort(ResourcesFactory.getInt(ServerConfig.KEY_INT_PORT) == null ? 51000
				: ResourcesFactory.getInt(ServerConfig.KEY_INT_PORT));
	}

	public SocketProvider(ServerSocket server) {
		this.serverSocket = server;
	}

	public void publish(Object object) throws Exception {
		String interfaceName = object.getClass().getInterfaces()[0].getName();
		ALL_OBJECT.put(interfaceName, object);
		LOGGER.info("-----------------publish starting");
		register.register(new Service(interfaceName, IPHelper.getIp(host), port));
	}

	public void publish(String packageName) throws Exception {
		LOGGER.info("-----------------publish starting");
		if (packageName == null) {
			throw new NullPointerException("packageName is null");
		}
		Set<Class<?>> classes = ClassUtils.getClasses(packageName);
		Iterator<Class<?>> iter = classes.iterator();
		Set<Service> servicees = new HashSet<Service>();
		while (iter.hasNext()) {
			Class<?> clazz = iter.next();
			if (!clazz.isInterface()) {
				servicees.add(new Service(clazz.getInterfaces()[0].getName(), IPHelper.getIp(host), port));
				ALL_OBJECT.put(clazz.getInterfaces()[0].getName(), clazz.newInstance());
			}
		}
		register.register(servicees);
	}

	public void start() throws Exception {
		MapHelper.print(ALL_OBJECT);
		bind(host, port);
	}

	private void checkSocket(String host, int port) throws Exception {
		if (this.serverSocket == null) {
			serverSocket = new ServerSocket();
		}
	}

	private void bind(String host, int port) throws Exception {
		checkSocket(host, port);
		SocketAddress endpoint = new InetSocketAddress(host, port);
		serverSocket.bind(endpoint);
		LOGGER.info("published...");
		LOGGER.info("server started on " + host + ":" + port + "...");
		Socket client = null;
		do {
			client = serverSocket.accept();
			LOGGER.debug("has a new client connected:" + client.getRemoteSocketAddress().toString());
			handler.handle(client);
		} while ((client = serverSocket.accept()) != null);
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return this.port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return this.host;
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public Register getRegister() {
		return this.register;
	}
}
