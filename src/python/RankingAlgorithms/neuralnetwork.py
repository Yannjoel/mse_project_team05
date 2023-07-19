import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

from ranker import Ranker
from RankingAlgorithms.feature_extractor import FeatureExtractor


class NeuralNetwork(nn.Module, Ranker):
    def __init__(self, n_features, n_hidden, n_out=5):
        super(NeuralNetwork, self).__init__()
        self.fc1 = nn.Linear(n_features, n_hidden)
        self.fc2 = nn.Linear(n_hidden, n_hidden)
        self.fc3 = nn.Linear(n_hidden, n_out)

    def forward(self, X):
        X = F.relu(self.fc1(X))
        X = F.relu(self.fc2(X))
        X = self.fc3(X)
        return X

    def get_scores(self, query, df):
        X = torch.tensor(
            FeatureExtractor(
                query=query, url=df["url"], title=df["title"], body=df["body"]
            ).get_features(),
            dtype=torch.float32,
        )
        return self.forward(X).numpy()

    def save(self, model_path="models/nn.pth"):
        torch.save(self, model_path)
        print("model saved in ", model_path)

    @staticmethod
    def load(model_path="models/nn.pth"):
        return torch.load(model_path)

    def train_loop(self, data, targets, loss_fn, optimizer):
        # create dataloader
        data = TensorDataset(data, targets)
        dataloader = DataLoader(data, batch_size=32, shuffle=True)
        size = len(dataloader.dataset)
        # train
        self.train()
        for batch, (X, y) in enumerate(dataloader):
            output = self.get_scores(X)
            loss = loss_fn(output, y)

            if batch % 500 == 0:
                loss_val, current = loss.item(), (batch + 1) * len(X)
                print(f"loss: {loss:>7f}  [{current:>5d}/{size:>5d}]")

            loss.backward()
            optimizer.step()
            optimizer.zero_grad()

        return loss.item()

    def test_loop(self, data, targets, loss_fn):
        # create dataloader
        data = TensorDataset(data, targets)
        dataloader = DataLoader(data, batch_size=32, shuffle=True)
        # test
        self.eval()
        test_loss = 0
        with torch.no_grad():
            for X, y in dataloader:
                output = self.get_scores(X)
                test_loss += loss_fn(output, y)

        test_loss /= len(dataloader)
        print(f"Test Error: Avg loss: {test_loss:>8f} \n")
