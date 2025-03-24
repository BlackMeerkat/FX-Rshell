from http.server import BaseHTTPRequestHandler, HTTPServer
import os
import cgi

class DownloadHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        try:
            file_path = os.path.join('/home/bl4ck-m33rk4t/Downloads/tmp', os.path.basename(self.path))
            if not os.path.exists(file_path):
                self.send_error(404, 'File not found')
                return
            self.send_response(200)
            self.send_header('Content-type', 'application/octet-stream')
            self.send_header('Content-Disposition', 'attachment; filename="' + os.path.basename(file_path) + '"')
            self.end_headers()
            with open(file_path, 'rb') as file:
                self.wfile.write(file.read())
        except Exception as e:
            print(f'Error serving file: {str(e)}')
            self.send_error(500, 'Internal server error')

    def do_POST(self):
        try:
            file_path = os.path.join('/home/bl4ck-m33rk4t/Downloads/tmp', os.path.basename(self.path))
            content_length = int(self.headers['Content-Length'])
            print(f'[+] Receiving file: {os.path.basename(self.path)}')
            print(f'[+] Content length: {content_length} bytes')

            with open(file_path, 'wb') as output_file:
                data = self.rfile.read(content_length)
                output_file.write(data)

            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(b'File uploaded successfully')
            print(f'[+] File saved as: {file_path}')

        except Exception as e:
            print(f'[-] Error receiving file: {str(e)}')
            self.send_error(500, 'Internal server error')

if __name__ == '__main__':
    server_address = ('192.168.1.7', 4444)
    try:
        httpd = HTTPServer(server_address, DownloadHandler)
        print(f'[+] HTTP server started on http://192.168.1.7:4444')
        print(f'[+] Serving directory: /home/bl4ck-m33rk4t/Downloads/tmp')
        print(f'[+] Server supports both upload (POST) and download (GET) operations')
        httpd.serve_forever()
    except Exception as e:
        print(f'[-] Server error: {str(e)}')
