package payload

// This class reassemble the network frames
class Message(header: Header, query: Question, answer: RRData, authority: RRData, additional: RRData) {}