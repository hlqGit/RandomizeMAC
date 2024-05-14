# A Tool to Randomize Your MAC
That's all it does. It only works on MacOS with arm architecture, sadly it is currently incompatible with Intel-based Macs. After Sonoma 14.4, the airport command-line tools were retired, making MAC randomizers much more complicated.

## How Does it Work?
By disabling and re-enabling wifi, there is a small point in time where WiFi is enabled, but also not connected to a network. That is the only time where you are able to change a MAC address. This program executes the commands in a very specific order so that your MAC address can change.

## Bugs? Errors? Reach Out!
There are numerous places you can reach me. You can submit an issue on the GitHub repo, or find my contact information on my profile. Please tell me any suggestions or recommendations! How can I make this better? Let me Know!

## FAQ's
**Does this program save my password?**
No. The password is only used for the sudo command in the terminal. It is NEVER sent anywhere else, and that is a key reason I made this program an open source program. You can check the PrimaryController java file and see how your password is used.

**Why does WiFi have to be disabled?**
By disabling and re-enabling WiFi, it allows the MAC address to be changed while it is reconnecting to your network.
