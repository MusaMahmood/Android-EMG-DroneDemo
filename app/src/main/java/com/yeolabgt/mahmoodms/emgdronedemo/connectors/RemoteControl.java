package com.yeolabgt.mahmoodms.emgdronedemo.connectors;

import android.os.Bundle;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * The base class for all remote control connections. This class is used to handle version
 * negotiation and transmitting commands via different networks.
 */
public abstract class RemoteControl {

    /**
     * Constants that indicate the current connection state
     */
    public enum ServiceState {
        /**
         * Disconnected
         */
        NONE,
        /**
         * Initiating an outgoing connection
         */
        CONNECTING,
        /**
         * Connected to a remote device
         */
        CONNECTED,
        /**
         * Error state, currently just used for callback handler
         */
        ERROR
    }

    /**
     * The possible types of errors that can be used to notify the user.
     */
    public enum ERROR_TYPES {
        /* Invalid version given */
        VERSION,
        /* Parsing error of the data sent by the server */
        PARSING
    }

    /**
     * Internal class that represents a json message transmitted via remote protocol.
     */
    protected class PresenterMessage {
        /**
         * The type of the message.
         */
        private final MessageType type;
        /**
         * Raw data string.
         */
        private final String data;

        PresenterMessage(MessageType type, String data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public String toString() {
            return "{" +
                    " \"type\": \"" + type.toString().toLowerCase(Locale.getDefault()) + "\"," +
                    " \"data\": \"" + data + "\"" +
                    "}";
        }
    }

    /**
     * The message type of the transmitted message.
     */
    private enum MessageType {
        /**
         * Version to be transmitted.
         */
        VERSION,
        /**
         * A command to be transmitted.
         */
        COMMAND
    }

    /**
     * The possible result values of the "connection" result sent to the handler.
     * Result will always contain a success state. If success is true, also a name is given.
     */
    public final static String[] RESULT_VALUES = { "success", "name", "error" };

    /**
     * The protocol version supported by our android client.
     */
    public final static ProtocolVersion CLIENT_PROTOCOL_VERSION = new ProtocolVersion(1, 1);

    /**
     * The resulting protocol version negotiated between server and client.
     */
    private ProtocolVersion mActiveProtocolVersion = null;

    /**
     * The current state of the remote control service.
     */
    protected ServiceState mState;

    /**
     * The callback handler that will handle displayed messages.
     */
    protected final Handler mHandler;

    /**
     * Creates a new remote control
     *
     * @param handler The handler that handles connection state changes and user notifications.
     */
    protected RemoteControl(Handler handler) {
        mState = ServiceState.NONE;
        this.mHandler = handler;
    }

    /**
     * Return the current connection state.
     */
    public synchronized ServiceState getState() {
        return mState;
    }

    /**
     * Sets the currently active protocol version to the version supported by both the
     * given server protocol version and the configured {@link #CLIENT_PROTOCOL_VERSION}.
     *
     * @param serverProtocolVersion The protocol version supported by the connected server.
     */
    private void setActiveProtocolVersion(ProtocolVersion serverProtocolVersion) {
        int minProtocolVersion = Math.max(serverProtocolVersion.getMinVersion(),
                CLIENT_PROTOCOL_VERSION.getMinVersion());

        int maxProtocolVersion = Math.min(serverProtocolVersion.getMaxVersion(),
                CLIENT_PROTOCOL_VERSION.getMaxVersion());

        mActiveProtocolVersion = new ProtocolVersion(minProtocolVersion, maxProtocolVersion);
    }

    /**
     * Parses a given message. The message needs to be a json string.
     *
     * @param sender The message sender.
     * @param message The message to parse.
     */
    public void handleMessage(String sender, String message) {
        try {
            JSONObject parser = new JSONObject(message);

            String type = parser.getString("type");
            String data = parser.getString("data");

            PresenterMessage msg =
                    new PresenterMessage(
                            MessageType.valueOf(type.toUpperCase(Locale.getDefault())), data);
            switch (msg.type) {
                case VERSION:
                    // Parse the version from given message
                    parser = new JSONObject(msg.data);
                    ProtocolVersion serverVersion = new ProtocolVersion(
                            parser.getInt("minVersion"), parser.getInt("maxVersion"));

                    setActiveProtocolVersion(serverVersion);

                    // Check if we have a common range of min and max versions. If not, disconnect.
                    if (mActiveProtocolVersion.getMinVersion()
                            > mActiveProtocolVersion.getMaxVersion()) {

                        android.os.Message userNotification
                                = mHandler.obtainMessage(ServiceState.ERROR.ordinal());
                        Bundle bundle = new Bundle();
                        bundle.putString(RESULT_VALUES[2], ERROR_TYPES.VERSION.toString());
                        userNotification.setData(bundle);
                        mHandler.sendMessage(userNotification);
                        this.mState = ServiceState.NONE;

                        this.disconnect();
                        break;
                    }

                    // We have a valid version range, so we are connected from now on.
                    this.mState = ServiceState.CONNECTED;

                    // Send the name of the connected device back to the result listener
                    android.os.Message userNotification
                            = mHandler.obtainMessage(ServiceState.CONNECTED.ordinal());
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(RESULT_VALUES[0], true);
                    bundle.putString(RESULT_VALUES[1], sender);
                    userNotification.setData(bundle);
                    mHandler.sendMessage(userNotification);

                    break;
            }

        } catch (JSONException e) {
            // Notify the user
            android.os.Message userNotification
                    = mHandler.obtainMessage(ServiceState.ERROR.ordinal());
            Bundle bundle = new Bundle();
            bundle.putString(RESULT_VALUES[2], ERROR_TYPES.PARSING.toString());
            userNotification.setData(bundle);
            mHandler.sendMessage(userNotification);

            e.printStackTrace();
        }
    }

    /**
     * Sends the given command to the presenter server.
     *
     * @param command The command to send.
     */
    public void sendCommand(Command command) {
        PresenterMessage toSend = new PresenterMessage(MessageType.COMMAND, command.getCommand());

        sendMessage(toSend);
    }

    /**
     * Will send a given message to the presenter server.
     *
     * @param message The message to send.
     */
    protected abstract void sendMessage(PresenterMessage message);

    /**
     * Will disconnect from the server.
     */
    protected abstract void disconnect();
}
