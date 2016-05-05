
import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

class ContactsContract
{
	static {
		AUTHORITY_URI = taintedUri();
	}

	class AggregationExceptions
	{
		static {
			CONTENT_URI = taintedUri();
		}
	}

	class SyncState
	{
		static {
			CONTENT_URI = taintedUri();
		}
	}

	class ProfileSyncState
	{
		static {
			CONTENT_URI = taintedUri();
		}
	}

    class Contacts
    {
		static { 
			CONTENT_URI = taintedUri();
			CONTENT_LOOKUP_URI = taintedUri();
			CONTENT_VCARD_URI = taintedUri();
			CONTENT_FILTER_URI = taintedUri();
			CONTENT_STREQUENT_URI = taintedUri();
			CONTENT_STREQUENT_FILTER_URI = taintedUri();
			CONTENT_GROUP_URI = taintedUri();
		}
    }

	class RawContacts
	{
		static { 
			CONTENT_URI = taintedUri();
		}
	}

	class StreamItems
	{
		static { 
			CONTENT_URI = taintedUri();
			CONTENT_PHOTO_URI = taintedUri();
			CONTENT_LIMIT_URI = taintedUri();
		}
	}

    class Profile
    {
		static { 
			CONTENT_URI = taintedUri();
			CONTENT_VCARD_URI = taintedUri();
			CONTENT_RAW_CONTACTS_URI = taintedUri();
		}
    }

	class Settings
	{
		static { 
			CONTENT_URI = taintedUri();
		}
	}

	class Data
	{
		static { 
			CONTENT_URI = taintedUri();
		}
	}

	class RawContactsEntity
	{
		static { 
			CONTENT_URI = taintedUri();
			PROFILE_CONTENT_URI = taintedUri();
		}
	}

	class PhoneLookup
	{
		static { 
			CONTENT_FILTER_URI = taintedUri();
		}
	}

	class StatusUpdates
	{
		static { 
			CONTENT_URI = taintedUri();
			PROFILE_CONTENT_URI = taintedUri();
		}
	}

	class CommonDataKinds
	{
		class Phone
		{
			static { 
				CONTENT_URI = taintedUri();
				CONTENT_FILTER_URI = taintedUri();
			}
		}

		class Email
		{
			static { 
				CONTENT_URI = taintedUri();
				CONTENT_FILTER_URI = taintedUri();
				CONTENT_LOOKUP_URI = taintedUri();
			}
		}
		
		class StructuredPostal
		{
			static { 
				CONTENT_URI = taintedUri();
			}
		}
	}

	class Groups
	{
		static {
			CONTENT_URI = taintedUri();
			CONTENT_SUMMARY_URI = taintedUri();
		}
	}

	class DataUsageFeedback
	{
		static { 
			FEEDBACK_URI = taintedUri();
		}
	}

	class DisplayPhoto
	{
		static {
			CONTENT_URI = taintedUri();
			CONTENT_MAX_DIMENSIONS_URI = taintedUri();
		}
	}
	
	@STAMP(flows = {@Flow(from="$CONTACTS",to="@return")})
	private static android.net.Uri taintedUri()
	{
		return new android.net.StampUri("");
	}
}
