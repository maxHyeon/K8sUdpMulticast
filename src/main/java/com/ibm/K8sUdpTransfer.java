package com.ibm;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class K8sUdpTransfer {

    public static void main(String[] args) throws Exception {
        String runMode = System.getenv("RUN_MODE");
        Configs configs = new Configs("local");
        String namespace = configs.getNamespace();
        String[] deployments = configs.getDeployments();
        int udpServerPort = configs.getUdpServerPort();

        String selfIp = getSelfIP();
        ExecutorService service = Executors.newFixedThreadPool(1);
        service.submit(new UdpReceiver(selfIp, udpServerPort,namespace,deployments)).get();

    }

    private static class UdpReceiver implements Runnable {

        private final byte[] buf = new byte[256];

        private final String addr;
        private final int port;
        private final String namespace;
        private final String[] deployments;

        UdpReceiver(String addr, int port,String namespace,String[] deployments) {
            this.addr = addr;
            this.port = port;
            this.namespace = namespace;
            this.deployments = deployments;
        }

        @Override
        public void run() {
            try (
                    DatagramSocket receiveSocket = new DatagramSocket(port);
                    DatagramSocket sendSocket = new DatagramSocket(port+1)
                ) {

                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    receiveSocket.receive(packet);
                    sendPacket(sendSocket, makeSendPacketList(packet, getPodAddresses("test", "test"), port+3));

                    //for debug
                    String selfIp = getSelfIP();
                    String received = new String(
                            packet.getData(), 0, packet.getLength());
                    if (!packet.getAddress().getHostAddress().equals(selfIp)) {
                        System.out.println(String.format("[%s] received '%s' from %s:%d",
                                selfIp,
                                received,
                                packet.getAddress(),
                                packet.getPort()));
//                        if ("end".equals(received)) {
//                            break;
//                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private List<DatagramPacket> makeSendPacketList(DatagramPacket receivedPacket, List<InetAddress> deliverList, int port) {
            List<DatagramPacket> sendPacketList = new ArrayList<>();
            for (InetAddress ip : deliverList) {
                sendPacketList.add(new DatagramPacket(receivedPacket.getData(), receivedPacket.getData().length, ip, port));
            }
            return sendPacketList;
        }

        private void sendPacket(DatagramSocket sendSocket, List<DatagramPacket> sendPacketList) {
            try {
                for (DatagramPacket sendPacket : sendPacketList) {
                    sendSocket.send(sendPacket);
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        private List<InetAddress> getPodAddresses(String namespace, String deployment) throws UnknownHostException {
            List<InetAddress> podAddressList = new ArrayList<>();
            podAddressList.add(InetAddress.getLocalHost());

            return podAddressList;
        }
    }

    private static String getSelfIP() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        return inetAddress.getHostAddress();
    }
}
