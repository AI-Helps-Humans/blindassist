ACTION_PROMPT = '''You are responsible for classifying user inputs based on their intent into five categories: "click", "fill in", "read", "summarize", "go to the website", and "query". Additionally, you need to extract the target object for each intent. This will help users navigate a website more efficiently. Below are the definitions and examples for each category:

1. **Click**: Inputs where the user intends to perform an action by clicking on a button, link, or other interactive elements. Extract the target of the click action.
   - Example 1: "Click the 'Submit' button."
     - Intent: Click
     - Target: 'Submit' button
   - Example 2: "Tap the 'Learn More' link."
     - Intent: Click
     - Target: 'Learn More' link
   - Example 3: "Press 'Next' to proceed to the following page."
     - Intent: Click
     - Target: 'Next' button

2. **Fill in**: Inputs where the user intends to enter or provide information into a form or text field. Extract the target field for filling in.
   - Example 1: "Enter your email address in the sign-up form."
     - Intent: Fill in
     - Target: email address field
   - Example 2: "Fill in your name and address in the required fields."
     - Intent: Fill in
     - Target: name and address fields
   - Example 3: "Provide your feedback in the comments section."
     - Intent: Fill in
     - Target: comments section

3. **Read**: Inputs where the user intends to read or review the entire content without interacting. Extract the content to be read.
   - Example 1: "Read the entire privacy policy before continuing."
     - Intent: Read
     - Target: privacy policy
   - Example 2: "Review the full FAQ section to find answers to common questions."
     - Intent: Read
     - Target: FAQ section
   - Example 3: "Look through the entire product description for more information."
     - Intent: Read
     - Target: product description

4. **Summarize**: Inputs where the user intends to get a brief summary or overview of content. Extract the content to be summarized.
   - Example 1: "Summarize the main points of the privacy policy."
     - Intent: Summarize
     - Target: privacy policy
   - Example 2: "Provide a brief overview of the FAQ section."
     - Intent: Summarize
     - Target: FAQ section
   - Example 3: "Give a quick summary of the product description."
     - Intent: Summarize
     - Target: product description

5. **Go to the website**: Inputs where the user intends to navigate to a specific website or web page. Extract the URL or website name.
   - Example 1: "Go to www.example.com for more details."
     - Intent: Go to the website
     - Target: www.example.com
   - Example 2: "Visit our homepage at www.homepage.com."
     - Intent: Go to the website
     - Target: www.homepage.com
   - Example 3: "Navigate to the contact page at www.contactpage.com."
     - Intent: Go to the website
     - Target: www.contactpage.com

6.	**Query**: Inputs where the user intends to check if a specified target and its sub-elements contain any clickable or fillable targets (hyperlinks or fillable text fields). Extract the target to be queried.
   - Example 1: "Check if the 'Contact Us' section contains any links or text fields."
     - Intent: Query
     - Target: 'Contact Us' section
   - Example 2: “Verify if the 'FAQ' section has any links or input fields.”
     - Intent: Query
	   - Target: 'FAQ' section
	 - Example 3: “Inspect the 'Registration' page for any interactive elements.”
     - Intent: Query
     - Target: 'Registration' page

Classify the following user inputs into one of the five categories: "click", "fill in", "read", "summarize", "go to the website", or "query" and extract the target object for each intent. Using this JSON schema:
      Result = {"intent": str, "target": str}
Return a Result
'''

SEARCH_WEB_PROMPT = '''Please provide the URL for the official website of "{}".
Using this JSON schema:
    Result = {{'url': str}}
Return a Result'''

SUMMARY_PROMPT = '''Please summarize the content of the "{}" provided in the following html text. Organize the summary into different categories based on the topics covered, Ensure that each category has a clear and concise heading, followed by a detailed summary of the relevant content.'''

SUMMARY_JSON_PROMPT = '''Extract the category names with its summary. 
Using this JSON schema:
    Result = {'category names': List[str], 'category summaries': List[str]}
Return a Result'''

SUMMARY_READ_CATEGORY_PROMPT = '''Read the category names to the user'''

READ_PROMPT_OLD = '''You are tasked with reading the content of the "{}" on a webpage from the following html text. Follow the steps below:
	1.	Locate and read the content of the "{}" on the webpage.
	2.	Present the content to the user in full.
'''

QUERY_PROMPT_1 = '''You are tasked with checking the object "{}" from the following html text to determine if it contains any clickable objects (with hyperlinks).
Using this JSON schema:
    Result = {{'object name': List[str], 'object urls': List[str]}}
Return a Result
'''
QUERY_PROMPT_2 = '''You are tasked with checking the object "{}" from the following html text to determine if it contains any fillable objects (with fillable text fields).
Using this JSON schema:
    Result = {{'object name': List[str], 'object hint text': List[str]}}
Return a Result
'''

EXTRACT_URL = '''Please ectract the url of the object "{}" from the following html text.
Using this JSON schema:
    Result = {{'url': str]}}
Return a Result
'''

EXTRACT_FILLOBJ = '''Please ectract the filed name and the hint text of the object "{}" from the following html text.
Using this JSON schema:
    Result = {{'field name': str, 'hint text':text}}
Return a Result
'''

EXTRACT_FILL_TEXT = '''Given the following user statement and the field they want to fill, extract the specific content that the user intends to provide for the field.

	1.	User Statement: “I need to update my profile with my new email address, which is john.doe@example.com.”
	2.	Field: “Email Address”

Response: [Extracted content]

In this example, the extracted content should be “john.doe@example.com” for the “Email Address” field.
Please ectract the specific content corresponding to the field "{}" from the following statement.
Using this JSON schema:
    Result = {{'field name': str, 'content':text}}
Return a Result
'''


EXTRACT_RELEVANT_CONTENT = '''Given an input from a user, generate a json-format dictionary with only the objects that fit the topic the user would like to query.
Input from user: {}
The original json-format dictinonary: 
{}
'''

READ_PROMPT = """
  Summarize all items provided in the HTML text below. Organize the output into a nested dictionary, where objects have parent and children relation based on HTML tags.
  For example:
    Object = {"summary": News, "content": A list of Australian News, "url": www.xxx.com, "children":
    [{"summary": Global outage caused by a 'defect' in a 'single content update', CrowdStrike says,
    "content": A currency shop screen shows the Windows error message ":(" as people line up CrowdStrike CEO George Kurtz says a global
    tech outage affecting various businesses was caused by a "defect" in a "single content update" for Windows hosts.',
    "url": www.xxx.com,}]}
     "children": [{...}]}
  Return an Object
  """