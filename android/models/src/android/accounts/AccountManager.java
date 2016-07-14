
class AccountManager
{
    private static AccountManager accountManager = new AccountManager();

    public static  android.accounts.AccountManager get(android.content.Context context) 
    { 
	return accountManager;
    }

    @STAMP(flows={@Flow(from="$ACCOUNTS",to="@return")})		
    private android.accounts.Account getAccount() {
    	return new Account(getAccountName(), new String());
    }

    @STAMP(flows={@Flow(from="$ACCOUNTS.Name",to="@return")})
	private java.lang.String getAccountName()
	{
		return new String();
	}

    // private static class StampAccountManagerFuture implements AccountManagerFuture<android.accounts.Account[]>
    // {

    // 	@STAMP(flows={@Flow(from="$ACCOUNTS",to="@return")})		
    //         public android.accounts.Account[] getResult() throws android.accounts.OperationCanceledException, java.io.IOException, android.accounts.AuthenticatorException
    //         {
    // 		Account act = this.getAccount(); 
    // 		Account[] actArray = new Accout[1]; 
    // 		actArray[0] = act; 
    // 		return actArray;
    // 	    }

    // }


    private static class StampAccountManagerFuture implements AccountManagerFuture<android.os.Bundle>
    {
	public boolean cancel(boolean mayInterruptIfRunning) { throw new RuntimeException("Stub!"); }
	public boolean isCancelled() { throw new RuntimeException("Stub!"); }
	public boolean isDone() { throw new RuntimeException("Stub!"); }

	@STAMP(flows={@Flow(from="$ACCOUNTS",to="@return")})		
	    public android.os.Bundle getResult() throws android.accounts.OperationCanceledException, java.io.IOException, android.accounts.AuthenticatorException
	    {
		return new android.os.Bundle();
	    }

	@STAMP(flows={@Flow(from="$ACCOUNTS",to="@return")})		
	    public android.os.Bundle getResult(long timeout, java.util.concurrent.TimeUnit unit) throws android.accounts.OperationCanceledException, java.io.IOException, android.accounts.AuthenticatorException
	    {
		return new android.os.Bundle();
	    }
    }

    private android.accounts.AccountManagerFuture<android.os.Bundle> registerAccountManagerCallback(final android.accounts.AccountManagerCallback<android.os.Bundle> callback)
    {
    	final StampAccountManagerFuture future = new StampAccountManagerFuture();
		callback.run(future);
    	return future;
    }

    public  android.accounts.AccountManagerFuture<android.os.Bundle> getAuthToken(android.accounts.Account account, java.lang.String authTokenType, android.os.Bundle options, android.app.Activity activity, android.accounts.AccountManagerCallback<android.os.Bundle> callback, android.os.Handler handler) 
    { 
    	return registerAccountManagerCallback(callback);
    }

    public  android.accounts.AccountManagerFuture<android.os.Bundle> getAuthToken(android.accounts.Account account, java.lang.String authTokenType, boolean notifyAuthFailure, android.accounts.AccountManagerCallback<android.os.Bundle> callback, android.os.Handler handler) 
    { 
    	return registerAccountManagerCallback(callback);
    }

    public  android.accounts.AccountManagerFuture<android.os.Bundle> addAccount(java.lang.String accountType, java.lang.String authTokenType, java.lang.String[] requiredFeatures, android.os.Bundle addAccountOptions, android.app.Activity activity, android.accounts.AccountManagerCallback<android.os.Bundle> callback, android.os.Handler handler) 
    { 
    	return registerAccountManagerCallback(callback);
    }

    public  android.accounts.AccountManagerFuture<android.os.Bundle> confirmCredentials(android.accounts.Account account, android.os.Bundle options, android.app.Activity activity, android.accounts.AccountManagerCallback<android.os.Bundle> callback, android.os.Handler handler) 
    { 
    	return registerAccountManagerCallback(callback);
    }

    public  android.accounts.AccountManagerFuture<android.os.Bundle> updateCredentials(android.accounts.Account account, java.lang.String authTokenType, android.os.Bundle options, android.app.Activity activity, android.accounts.AccountManagerCallback<android.os.Bundle> callback, android.os.Handler handler) 
    { 
    	return registerAccountManagerCallback(callback);
    }

    public  android.accounts.AccountManagerFuture<android.os.Bundle> editProperties(java.lang.String accountType, android.app.Activity activity, android.accounts.AccountManagerCallback<android.os.Bundle> callback, android.os.Handler handler) 
    { 
    	return registerAccountManagerCallback(callback);
    }

    public  android.accounts.AccountManagerFuture<android.os.Bundle> getAuthTokenByFeatures(java.lang.String accountType, java.lang.String authTokenType, java.lang.String[] features, android.app.Activity activity, android.os.Bundle addAccountOptions, android.os.Bundle getAuthTokenOptions, android.accounts.AccountManagerCallback<android.os.Bundle> callback, android.os.Handler handler) 
    { 
    	return registerAccountManagerCallback(callback);
    }

    public  android.accounts.Account[] getAccounts() { 
    	Account act = getAccount(); 
    	Account[] actArray = new Account[1]; 
    	actArray[0] = act; 
    	return actArray;
    }

    public  android.accounts.Account[] getAccountsByType(java.lang.String type) { 
    	Account act = getAccount(); 
    	Account[] actArray = new Account[1]; 
    	actArray[0] = act; 
    	return actArray;
    }


    // private android.accounts.AccountManagerFuture<android.accounts.Account[]> registerAccountManagerCallback(final android.accounts.AccountManagerCallback<android.accounts.Account[]> callback)
    // {
    // 	final StampAccountManagerFuture future = new StampAccountManagerFuture();
    // 	edu.stanford.stamp.harness.ApplicationDriver.getInstance().
    // 	    registerCallback(new edu.stanford.stamp.harness.Callback(){
    // 		    public void run() {
    // 			callback.run(future);
    // 		    }
    // 		});
    // 	return future;
    // }



    // public  android.accounts.AccountManagerFuture<android.accounts.Account[]> getAccountsByTypeAndFeatures(java.lang.String type, java.lang.String[] features, 
    // 													   android.accounts.AccountManagerCallback<android.accounts.Account[]> callback, 
    // 													   android.os.Handler handler) {
    // 	return new registerAccountManagerCallback(callback);
    // }

    // public  java.lang.String blockingGetAuthToken(android.accounts.Account account, java.lang.String authTokenType, 
    // 						  boolean notifyAuthFailure) throws android.accounts.OperationCanceledException, 
    // 										    java.io.IOException, android.accounts.AuthenticatorException { 
    // 	throw new RuntimeException("Stub!"); 
    // }

}
