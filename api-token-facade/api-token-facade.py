from flask import Flask, request, render_template_string, jsonify, redirect, url_for
import requests
import random
import string

app = Flask(__name__)

# Configuration
NEXUS_API_URL = f"{os.environ['NEXUS_URL']}/v1/security/users"
ADMIN_USERNAME = os.environ['ADMIN_USER']
ADMIN_PASSWORD = os.environ['ADMIN_PASSWORD']

def generate_api_token(length=32):
    characters = string.ascii_letters + string.digits + string.punctuation
    return ''.join(random.choice(characters) for i in range(length))

@app.route('/', methods=['GET'])
def index():
    preferred_username = request.headers.get('X-Forwarded-Preferred-Username')
    if not preferred_username:
        return jsonify({"error": "Unauthorized access!"}), 401

    return render_template_string('''
        <!DOCTYPE html>
        <html>
        <head>
            <title>API Token Reset</title>
        </head>
        <body>
            <h2>Welcome, {{ preferred_username }}</h2>
            <p>This process will generate a new API token for you. Please note that you will only see this token once. Make sure to save it securely.</p>
            <form action="/" method="post">
                <input type="submit" value="Generate New API Token">
            </form>
        </body>
        </html>
    ''', preferred_username=preferred_username)

@app.route('/', methods=['POST'])
def reset_api_token():
    preferred_username = request.headers.get('X-Forwarded-Preferred-Username')
    if not preferred_username:
        return jsonify({"error": "Unauthorized access!"}), 401

    new_api_token = generate_api_token()

    # Update the user's API token in Nexus
    response = requests.put(f"{NEXUS_API_URL}/{preferred_username}/change-password", 
                            data=new_api_token, 
                            auth=(ADMIN_USERNAME, ADMIN_PASSWORD),
                            headers={"Content-Type": "text/plain"})

    if response.status_code == 204:  # Success
        return render_template_string('''
            <!DOCTYPE html>
            <html>
            <head>
                <title>API Token Generated</title>
            </head>
            <body>
                <p>Your new API token is: <strong>{{ new_api_token }}</strong></p>
                <p><strong>Important:</strong> Save this token now. You will not be able to see it again.</p>
            </body>
            </html>
        ''', new_api_token=new_api_token)
    else:
        # Handle errors as before
        return jsonify({"error": "Failed to update API token"}), 500

if __name__ == '__main__':
    app.run(debug=True)