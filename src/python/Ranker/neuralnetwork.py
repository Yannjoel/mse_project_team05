import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim


class NeuralNetwork(nn.Module):

    def __init__(self, n_features, n_hidden)
        super(NeuralNetwork, self).__init__()
        self.fc1 = nn.Linear(n_features, n_hidden)
        self.fc2 = nn.Linear(n_hidden, 1)

    def forward(self, x):
        x = F.relu(self.fc1(x))
        x = self.fc2(x)
        return x
    
    def save(self, model_path='nn.joblib'):
        torch.save(self.state_dict(), model_path)
        print('model saved in ', model_path)

    def load(self, model_path='nn.joblib'):
        self.load_state_dict(torch.load(model_path))
        return self.model
    
    def fit(self, X, y):
