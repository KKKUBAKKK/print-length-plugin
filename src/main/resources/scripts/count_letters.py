import sys

def count_letters(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
            words = content.split()
            for words in words:
                print(f"Word: '{words}', Length: {len(words)}")
            # letter_count = sum(1 for char in content if char.isalpha())
            # print(f"Number of letters in the file: {letter_count}")
    except Exception as e:
        print(f"Error reading file: {str(e)}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python count_letters.py <file_path>")
        sys.exit(1)

    count_letters(sys.argv[1])