
/**Fehlercodes und codes die zu Nachrichten vom Server zurückgesendet werden
 * 
 * Codes 0-99 sind sog. "Status Codes". Das sind keine Fehlercodes.
 * Codes 100-? sind Fehlercodes. Sie deuten auf einen bestimmten Fehler hin*/
public interface Codes {
	
	//Codes 0-99 keine Errors
	public static final int CODE_SUCCESS = 0;
	//Einkommende Datei
	public static final int CODE_READY = 1;
	//Ausgehende Datei
	public static final int CODE_SEND_READY = 2;
	
	//Codes 100-... Error codes
	public static final int CODE_UNKNOWN_ERROR = 100;
	public static final int CODE_MISSING_PAYLOAD = 101;
	public static final int CODE_NOT_AUTHENTICATED = 102;
	public static final int CODE_INSUFICCIENT_PERMISSIONS = 103;
	public static final int CODE_ARGUMENT_PARSE_ERROR = 104;
	public static final int CODE_ID_NOT_MATCH = 105;
	public static final int CODE_HTTP_ERROR = 106;
	public static final int CODE_ENTRY_MISSING = 107;
	public static final int CODE_FILE_MISSING = 108;
}
