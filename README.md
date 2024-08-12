# blindassist

# Description
## Introduction to TwilightNav

Hello there! I'm excited to introduce you to **TwilightNav**: a mobile app specifically designed to help visually impaired individuals navigate the internet. This app is a tool designed for blind or visually impaired users to navigate the internet. It offers a combination of voice commands and gesture controls, making web browsing more accessible and user-friendly. With features like web content analysis, voice controlling, and gesture-based navigation, this app helps users to surf the web easily. Our goal is to make the internet simply and effectively accessible to everyone.

Alright, let's dive into the details of this incredible app, which is designed to make web browsing fully accessible for blind or visually impaired users.

### Web Content Analysis

The first feature I want to introduce is **Web Content Analysis**. This app uses advanced technology to break down and analyze web pages. By leveraging the **Gimini API**, it performs semantic analysis on the HTML of a webpage. The HTML is also converted into a JSON format, allowing for a detailed structural analysis. This combination of semantic and structural analysis results in a tree-like structure of the webpage content. Each node in this structure represents a part of the webpage and contains detailed information about itself, as well as summary information for all of its child nodes. This hierarchical organization makes it easier to understand and navigate complex web pages.

### Voice Control

The second feature is **Voice Control**, making it possible to interact with the web entirely through spoken commands. To use this feature, you simply long press on the screen to start inputting your voice command. The app then processes your input using the **Gemini API**, identifying your intent and the target on the page to which the command applies. The app recognizes six main intents:

1. **Go to the website**: Opens a specific website as per your command.
2. **Summarize**: Reads the summary of a target, which could be the entire webpage or a specific section of it.
3. **Read**: Provides detailed information about a target, reading through its content and all its descendants.
4. **Query**: Searches for clickable and fillable elements within a target, helping you navigate interactive parts of the webpage.
5. **Click**: Simulates a click on a target, like following a link to a new page.
6. **Fill in**: Allows you to fill out a form or submit a comment within a target.

In addition, both "Read" and "Query" will generate a tree-like playback text based on the tree-like structure of the webpage. This kind of playback text includes the detailed content of the selected object and all its sub-elements. Users can then navigate through this content using gesture controls, selecting which parts of the content they want to listen to or skip.

### Gesture Control Modes

Finally, this app provides two different gesture control modes depending on whether the playback text is ready.

#### Playback Mode (when the playback text is ready):
- **Single tap**: Pause or resume the audio.
- **Double tap**: Exit the audio playback mode.
- **Long press**: Exit playback mode and start voice input.
- **Swipe up**: Play the content of the parent element.
- **Swipe down**: Play the content of the child element.
- **Swipe left**: Play the content of the left sibling element.
- **Swipe right**: Play the content of the right sibling element.

#### Non-Playback Mode (when the playback text is empty):
- **Single tap**: Decrease the speech rate.
- **Double tap**: Increase the speech rate.
- **Long press**: Start inputting a voice command.
- **Swipe up**: Scroll up the webpage.
- **Swipe down**: Scroll down the webpage.
- **Swipe left**: Navigate to the previous page.
- **Swipe right**: Navigate to the next page.

These gestures make the app highly versatile and powerful, providing a rich, accessible browsing experience.

# Usage
- Download the code
- Prepare the google client auth file: app/src/main/res/raw/real_app_cred.json
- Fill in a gemini API key in app/src/main/python/agent.py
- Compile and use


# Contribution

1. Yan Li (The University of Sydney)
2. Tianyi Zhang (The University of Melbourne)
3. Yue Dai (The University of Western Australia)
4. Zhen Zhou (Zhuhai KINGSOFT Office Software Co., Ltd)