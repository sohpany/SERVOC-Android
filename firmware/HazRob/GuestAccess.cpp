#include "Hardware.h"
#include <Preferences.h>

namespace {
constexpr char KEY[]="guestToken";
String loadToken(){Preferences p;if(!p.begin(Cfg::NVS,true))return String();String s=p.getString(KEY,"");p.end();return s;}
}

bool guestAccessSet(const String& token){String t=token;t.trim();if(t.length()<20||t.length()>128)return false;Preferences p;if(!p.begin(Cfg::NVS,false))return false;size_t n=p.putString(KEY,t);p.end();return n==t.length();}
void guestAccessRevoke(){Preferences p;if(!p.begin(Cfg::NVS,false))return;p.remove(KEY);p.end();}
bool guestAccessValid(const String& token){String expected=loadToken();if(expected.isEmpty()||token.length()!=expected.length())return false;uint8_t diff=0;for(size_t i=0;i<expected.length();i++)diff|=(uint8_t)(expected[i]^token[i]);return diff==0;}
bool guestAccessEnabled(){return !loadToken().isEmpty();}
